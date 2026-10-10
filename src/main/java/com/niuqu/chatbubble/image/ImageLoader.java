package com.niuqu.chatbubble.image;

import com.niuqu.chatbubble.E33Log;
import com.niuqu.chatbubble.compat.TextureCompat;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public final class ImageLoader {
    private static final Map<String, ImageEntry> CACHE = new ConcurrentHashMap<>();
    private static final Deque<String> LRU = new ArrayDeque<>();
    private static final Deque<ImageEntry> PENDING = new ArrayDeque<>();

    private static final java.util.concurrent.ExecutorService EXEC =
        java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "e33chat-image");
            t.setDaemon(true);
            return t;
        });

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public static HttpClient client() { return CLIENT; }

    public static java.util.concurrent.ExecutorService executor() { return EXEC; }

    private static final int MAX_RECEIVE_BYTES = 16 * 1024 * 1024;

    private static final long REQUEST_TIMEOUT_SECONDS = 60;

    static final int RATE_LIMIT_PER_WINDOW = 4;
    static final long RATE_WINDOW_MS = 10_000;
    static final int QUEUE_CAP = 32;
    static final int CACHE_CAP = 64;

    static final int CARD_W = 512;
    static final int CARD_H = 512;

    private static final long FAILED_RETRY_MS = 120_000;
    private static final Deque<Long> RECENT_STARTS = new ArrayDeque<>();
    private static volatile boolean enabled = true;

    public static final java.util.concurrent.atomic.AtomicInteger VERSION = new java.util.concurrent.atomic.AtomicInteger();

    private ImageLoader() {}

    public static int version() { return VERSION.get(); }

    public static void setEnabled(boolean e) {
        enabled = e;
        if (!e) {
            MinecraftClient.getInstance().execute(() -> {
                TextureManager tm = MinecraftClient.getInstance().getTextureManager();
                for (String u : CACHE.keySet()) {
                    tm.destroyTexture(Identifier.of("e33chat", "img/" + hash(u)));
                }
                CACHE.clear();
                LRU.clear();
                PENDING.clear();
            });
        }
    }

    public static ImageEntry getOrLoad(String url) {
        if (!enabled) return null;
        ImageEntry entry = CACHE.get(url);
        if (entry == null) {
            entry = CACHE.computeIfAbsent(url, ImageLoader::startLoad);
        } else if (entry.state() == ImageEntry.State.FAILED
                && System.currentTimeMillis() - entry.failedAtMillis() > FAILED_RETRY_MS) {

            ImageEntry fresh = new ImageEntry(url);
            if (CACHE.replace(url, entry, fresh)) {
                startLoadInto(url, fresh);
                entry = fresh;
            }
        }
        touchLru(url);
        return entry;
    }

    private static void touchLru(String url) {
        synchronized (LRU) {
            LRU.remove(url);
            LRU.addLast(url);
        }
        evictIfNeeded();
    }

    private static void evictIfNeeded() {
        synchronized (LRU) {
            if (LRU.size() <= CACHE_CAP) return;
            Iterator<String> it = LRU.iterator();
            while (it.hasNext() && LRU.size() > CACHE_CAP) {
                String url = it.next();
                ImageEntry e = CACHE.get(url);

                if (e == null || e.state() == ImageEntry.State.LOADING) continue;
                it.remove();
                CACHE.remove(url, e);
                if (e.state() == ImageEntry.State.LOADED && e.textureId() != null) {
                    Identifier id = e.textureId();
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
                    });
                }
                VERSION.incrementAndGet();
            }
        }
    }

    public static boolean isUsableUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.toLowerCase();
        if (lower.startsWith("e33chat://")) {

            return lower.startsWith("e33chat://media/")
                && com.niuqu.chatbubble.server.DiskMediaStore.isValidMediaId(
                    url.substring("e33chat://media/".length()));
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false;
        try {
            URI uri = new URI(url);
            return uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static int[] scaledSize(int w, int h) {
        if (w <= 0 || h <= 0) return new int[]{1, 1};
        if (w <= CARD_W && h <= CARD_H) return new int[]{w, h};
        double scale = Math.min((double) CARD_W / w, (double) CARD_H / h);
        return new int[]{
            Math.max(1, (int) (w * scale)),
            Math.max(1, (int) (h * scale))
        };
    }

    private static ImageEntry startLoad(String url) {
        ImageEntry entry = new ImageEntry(url);
        startLoadInto(url, entry);
        return entry;
    }

    private static void startLoadInto(String url, ImageEntry entry) {
        if (!isUsableUrl(url)) {
            entry.markFailed("bad url");
            return;
        }
        if (tryAcquireSlot()) {
            launchFetch(url, entry);
        } else {
            synchronized (PENDING) {
                if (PENDING.size() < QUEUE_CAP) {
                    PENDING.addLast(entry);

                } else {
                    entry.markFailed("rate limited");
                }
            }
        }
    }

    private static boolean tryAcquireSlot() {
        long now = System.currentTimeMillis();
        synchronized (RECENT_STARTS) {
            while (!RECENT_STARTS.isEmpty() && now - RECENT_STARTS.peekFirst() > RATE_WINDOW_MS) {
                RECENT_STARTS.removeFirst();
            }
            if (RECENT_STARTS.size() >= RATE_LIMIT_PER_WINDOW) return false;
            RECENT_STARTS.addLast(now);
            return true;
        }
    }

    public static void tick() {
        if (!enabled) return;
        while (true) {
            ImageEntry next;
            synchronized (PENDING) {
                if (PENDING.isEmpty()) return;
                next = PENDING.peekFirst();
            }
            if (next.state() != ImageEntry.State.LOADING) {
                synchronized (PENDING) { PENDING.removeFirst(); }
                continue;
            }
            if (!tryAcquireSlot()) return;
            synchronized (PENDING) { PENDING.removeFirst(); }
            launchFetch(next.url(), next);
        }
    }

    private static void launchFetch(String url, ImageEntry entry) {
        CompletableFuture.runAsync(() -> fetchAndDecode(url, entry), EXEC)
            .orTimeout(REQUEST_TIMEOUT_SECONDS + 5, TimeUnit.SECONDS)
            .exceptionally(t -> {

                E33Log.info("[e33chat] image fetch {} -> future timeout after {}s (download may still finish)",
                    url, REQUEST_TIMEOUT_SECONDS + 5);
                return null;
            });
    }

    private static void fetchAndDecode(String url, ImageEntry entry) {
        long t0 = System.currentTimeMillis();
        try {
            byte[] body;
            long t1;
            if (url.startsWith("e33chat://")) {
                body = MediaClient.fetch(url.substring("e33chat://media/".length()));
                t1 = System.currentTimeMillis();
                if (body == null) {
                    entry.markFailed("server media fetch failed");
                    E33Log.info("[e33chat] image fetch {} -> server media fetch failed ({}ms)",
                        url, t1 - t0);
                    return;
                }
            } else {
                HttpResponse<byte[]> resp = CLIENT.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
                t1 = System.currentTimeMillis();
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    entry.markFailed("http " + resp.statusCode());
                    E33Log.info("[e33chat] image fetch {} -> HTTP {} ({}ms)", url, resp.statusCode(), t1 - t0);
                    return;
                }
                body = resp.body();
                if (body == null || body.length == 0 || body.length > MAX_RECEIVE_BYTES) {
                    entry.markFailed("empty or too large");
                    E33Log.info("[e33chat] image fetch {} -> bad body {} bytes ({}ms)", url, body == null ? 0 : body.length, t1 - t0);
                    return;
                }
            }
            RasterImageDecoder.DecodedImage decoded = RasterImageDecoder.decode(body);
            if (decoded == null) {
                entry.markFailed("unsupported format");
                E33Log.info("[e33chat] image fetch {} -> decode failed ({} bytes, {}ms)", url, body.length, t1 - t0);
                return;
            }

            int[] sc = scaledSize(decoded.width(), decoded.height());
            if (sc[0] != decoded.width() || sc[1] != decoded.height()) {
                NativeImage scaled = new NativeImage(NativeImage.Format.RGBA, sc[0], sc[1], false);
                try {
                    decoded.image().resizeSubRectTo(0, 0, decoded.width(), decoded.height(), scaled);
                } catch (Throwable t) {
                    scaled.close();
                    decoded.image().close();
                    entry.markFailed("scale: " + t);
                    E33Log.info("[e33chat] image fetch {} -> scale failed: {}", url, t.toString());
                    return;
                }
                decoded.image().close();
                decoded = new RasterImageDecoder.DecodedImage(scaled, sc[0], sc[1]);
            }
            E33Log.info("[e33chat] image fetch {} -> {}x{} ({} bytes, {}ms)",
                url, decoded.width(), decoded.height(), body.length, t1 - t0);

            final RasterImageDecoder.DecodedImage uploadImage = decoded;
            MinecraftClient.getInstance().execute(() -> {
                if (entry.state() != ImageEntry.State.LOADING) {
                    uploadImage.image().close();
                    E33Log.info("[e33chat] image upload SKIPPED (state {}) for {}", entry.state(), url);
                    return;
                }

                try {
                    Identifier id = Identifier.of("e33chat", "img/" + hash(url));
                    TextureManager tm = MinecraftClient.getInstance().getTextureManager();
                    tm.destroyTexture(id);
                    tm.registerTexture(id, TextureCompat.create(id.getPath(), uploadImage.image()));
                    entry.markLoaded(id, uploadImage.image());
                    E33Log.info("[e33chat] image upload OK {} -> {}x{} @ {}", url, entry.width(), entry.height(), id);
                } catch (Throwable t) {
                    entry.markFailed("upload: " + t);
                    E33Log.info("[e33chat] image upload FAILED {}: {}", url, t.toString());
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            entry.markFailed("interrupted");
        } catch (Throwable t) {
            long t1 = System.currentTimeMillis();
            E33Log.info("[e33chat] image fetch {} -> exception ({}ms): {}", url, t1 - t0, t.toString());
            entry.markFailed(String.valueOf(t));
        }
    }

    private static String hash(String url) {
        return Integer.toHexString(url.hashCode());
    }

    static Map<String, ImageEntry> cache() { return CACHE; }
    static void clearCacheForTest() {
        CACHE.clear();
        synchronized (LRU) { LRU.clear(); }
        synchronized (PENDING) { PENDING.clear(); }
        synchronized (RECENT_STARTS) { RECENT_STARTS.clear(); }
    }
    static boolean tryAcquireSlotForTest() { return tryAcquireSlot(); }
}
