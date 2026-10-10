package com.niuqu.chatbubble.image;

import com.niuqu.chatbubble.E33Log;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AnimatedImageLoader {
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_FRAMES = 120;
    private static final int MAX_DIMENSION = 512;
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private static final long MAX_PIXELS_PER_IMAGE = 8L * 1024L * 1024L;
    private static final long FAILED_RETRY_MS = 60_000;

    private static final java.util.Deque<String> LRU = new java.util.ArrayDeque<>();

    private static final int CACHE_CAP = 24;

    private static final int FRAME_BUDGET = 192;

    private static final long IDLE_EVICT_MS = 5 * 60_000;

    private static void touchLru(String url) {
        synchronized (LRU) {
            LRU.remove(url);
            LRU.addLast(url);
        }
        evictIfNeeded();
    }

    private static void evictIfNeeded() {
        synchronized (LRU) {
            long now = System.currentTimeMillis();
            int totalFrames = 0;
            for (String url : LRU) {
                Entry e = CACHE.get(url);
                if (e != null) totalFrames += frameCount(e);
            }
            Iterator<String> it = LRU.iterator();
            while (it.hasNext() && (LRU.size() > CACHE_CAP || totalFrames > FRAME_BUDGET)) {
                String url = it.next();
                Entry e = CACHE.get(url);
                if (e == null) {
                    it.remove();
                    continue;
                }

                boolean loading = !e.ready && !e.failed && !e.staticImage;
                if (loading || e.staticImage || now - e.lastAccessMs < IDLE_EVICT_MS) continue;
                it.remove();
                CACHE.remove(url, e);
                destroyEntryTextures(e);
                totalFrames -= frameCount(e);
            }
        }
    }

    private static int frameCount(Entry e) {
        Identifier[] frames = e.frames;
        return frames == null ? 0 : frames.length;
    }

    private static void destroyEntryTextures(Entry e) {
        Identifier[] frames = e.frames;
        e.frames = null;
        e.ready = false;
        if (frames == null) return;
        MinecraftClient.getInstance().execute(() -> {
            var tm = MinecraftClient.getInstance().getTextureManager();
            for (Identifier id : frames) {
                if (id == null) continue;
                try {
                    tm.destroyTexture(id);
                } catch (Throwable ignored) {}
            }
        });
    }

    private static final ExecutorService EXEC =
        Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "e33chat-animated");
            t.setDaemon(true);
            return t;
        });

    private AnimatedImageLoader() {}

    public static boolean looksAnimated(String url, String nameHint) {
        String lower = (url + " " + (nameHint == null ? "" : nameHint))
            .toLowerCase(java.util.Locale.ROOT);
        boolean formatHint = lower.contains("format=gif") || lower.contains("format=webp");
        int query = lower.indexOf('?');
        if (query >= 0) lower = lower.substring(0, query);
        return lower.endsWith(".gif") || lower.endsWith(".webp")
            || lower.endsWith(".apng") || formatHint;
    }

    public static Entry getOrLoad(String url, String nameHint) {
        if (url == null || url.isBlank() || !looksAnimated(url, nameHint)) return null;
        return cachedOrStart(url);
    }

    public static Entry getOrLoadAny(String url, String nameHint) {
        if (url == null || url.isBlank()) return null;
        return cachedOrStart(url);
    }

    public static Entry getOrLoadFile(java.io.File file) {
        if (file == null || !file.isFile()) return null;
        return getOrLoad(file.toURI().toString(), file.getName());
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        for (Entry entry : CACHE.values()) {
            if (now - entry.lastAccessMs > 30_000) continue;
            entry.advance(now);
        }
        evictIfNeeded();
    }

    private static Entry cachedOrStart(String url) {
        Entry current = CACHE.get(url);
        if (current != null && current.failed
                && System.currentTimeMillis() - current.failedAt > FAILED_RETRY_MS) {
            Entry retry = new Entry(url);
            if (CACHE.replace(url, current, retry)) {
                touchLru(url);
                EXEC.execute(() -> load(retry));
                return retry;
            }
        }
        Entry entry = CACHE.computeIfAbsent(url, AnimatedImageLoader::start);
        entry.lastAccessMs = System.currentTimeMillis();
        touchLru(url);
        return entry;
    }

    private static Entry start(String url) {
        Entry entry = new Entry(url);
        EXEC.execute(() -> load(entry));
        return entry;
    }

    private static void load(Entry entry) {
        try {
            byte[] bytes;
            if (entry.url.startsWith("file:")) {
                bytes = java.nio.file.Files.readAllBytes(
                    java.nio.file.Path.of(URI.create(entry.url)));
            } else if (entry.url.startsWith("e33chat://media/")) {
                bytes = MediaClient.fetch(entry.url.substring("e33chat://media/".length()));
                if (bytes == null) {
                    entry.markFailed();
                    return;
                }
            } else {
                URI requestUri = URI.create(URI.create(entry.url).toASCIIString());
                HttpResponse<byte[]> response = ImageLoader.client().send(
                    HttpRequest.newBuilder(requestUri)
                        .timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
                bytes = response.statusCode() >= 200 && response.statusCode() < 300
                    ? response.body() : null;
                if (bytes == null) {
                    entry.markFailed();
                    E33Log.info("[e33chat] animated image fetch failed: {} (HTTP {})",
                        entry.url, response.statusCode());
                    return;
                }
            }
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                entry.markFailed();
                E33Log.info("[e33chat] animated image rejected: {} ({} bytes)",
                    entry.url, bytes.length);
                return;
            }
            entry.sizeBytes = bytes.length;
            Decoded decoded = decode(bytes);
            if (decoded == null || decoded.frames().size() < 2) {
                entry.staticImage = true;
                return;
            }
            MinecraftClient.getInstance().execute(() -> {
                try {
                    Identifier[] ids = new Identifier[decoded.frames().size()];
                    for (int i = 0; i < decoded.frames().size(); i++) {
                        Identifier id = Identifier.of("e33chat",
                        "anim/" + Integer.toHexString(entry.url.hashCode()) + "_" + i);
                        MinecraftClient.getInstance().getTextureManager().registerTexture(
                            id, com.niuqu.chatbubble.compat.TextureCompat.create(
                                id.getPath(), decoded.frames().get(i)));
                        ids[i] = id;
                    }
                    entry.frames = ids;
                    entry.delays = decoded.delays();
                    entry.width = decoded.width();
                    entry.height = decoded.height();
                    entry.ready = true;
                    entry.frameStart = System.currentTimeMillis();
                    ImageLoader.VERSION.incrementAndGet();
                } catch (Throwable t) {
                    entry.markFailed();
                    E33Log.debug("[e33chat] animated image texture upload failed: {}", t.toString());
                }
            });
        } catch (Throwable t) {
            entry.markFailed();
            E33Log.info("[e33chat] animated image load failed: {} -> {}", entry.url, t.toString());
        }
    }

    public record Probe(int frames, int width, int height, String format) {}

    public enum OverBudget { TOO_MANY_FRAMES, TOO_LARGE_DIMENSION, TOO_LARGE_BYTES }

    public static Probe probe(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, false);
                int count = reader.getNumImages(true);
                if (count < 2) return null;
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);

                if ("gif".equalsIgnoreCase(reader.getFormatName())) {
                    IIOMetadata stream = reader.getStreamMetadata();
                    if (stream != null) {
                        try {
                            var root = stream.getAsTree("javax_imageio_gif_stream_1.0");
                            if (root instanceof IIOMetadataNode node) {
                                var d = node.getElementsByTagName("LogicalScreenDescriptor");
                                if (d.getLength() > 0 && d.item(0) instanceof IIOMetadataNode n) {
                                    w = Math.max(w, parseInt(n.getAttribute("logicalScreenWidth"), w));
                                    h = Math.max(h, parseInt(n.getAttribute("logicalScreenHeight"), h));
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
                return new Probe(count, w, h, reader.getFormatName().toLowerCase(java.util.Locale.ROOT));
            } finally {
                reader.dispose();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static OverBudget checkBudget(Probe probe, long byteCount) {
        if (probe == null) return null;
        if (probe.frames() > MAX_FRAMES) return OverBudget.TOO_MANY_FRAMES;
        if (probe.width() > MAX_DIMENSION || probe.height() > MAX_DIMENSION) {
            return OverBudget.TOO_LARGE_DIMENSION;
        }
        if (byteCount > MAX_BYTES) return OverBudget.TOO_LARGE_BYTES;
        return null;
    }

    public static String mimeType(String format) {
        if (format == null) return "application/octet-stream";
        return switch (format.toLowerCase(java.util.Locale.ROOT)) {
            case "gif" -> "image/gif";
            case "png" -> "image/png";
            case "jpeg", "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static int framesWithinBudget(int width, int height) {
        long px = (long) Math.max(1, width) * Math.max(1, height);
        if (px <= 0) return MAX_FRAMES;
        long cap = MAX_PIXELS_PER_IMAGE / px;
        if (cap > MAX_FRAMES) return MAX_FRAMES;
        return (int) Math.max(1, cap);
    }

    private static Decoded decode(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, false);
                int count = Math.min(MAX_FRAMES, reader.getNumImages(true));
                if (count < 2) return null;
                if ("gif".equalsIgnoreCase(reader.getFormatName())) {
                    return decodeGif(reader, count);
                }

                int budget = framesWithinBudget(reader.getWidth(0), reader.getHeight(0));
                if (budget < count) {
                    E33Log.info("[e33chat] animated image trimmed to {} of {} frames (pixel budget)",
                        budget, count);
                    count = budget;
                }
                ArrayList<NativeImage> frames = new ArrayList<>();
                int[] delays = new int[count];
                int width = 0, height = 0;
                try {
                    for (int i = 0; i < count; i++) {
                        var frame = reader.read(i);
                        if (frame == null || frame.getWidth() <= 0 || frame.getHeight() <= 0
                            || frame.getWidth() > MAX_DIMENSION || frame.getHeight() > MAX_DIMENSION) break;
                        width = frame.getWidth();
                        height = frame.getHeight();
                        frames.add(RasterImageDecoder.fromBufferedImage(frame));
                        delays[i] = frameDelay(reader.getImageMetadata(i));
                    }
                } catch (Throwable t) {
                    closeFrames(frames);
                    throw t;
                }
                if (frames.size() < 2) {
                    for (NativeImage image : frames) image.close();
                    return null;
                }
                for (int i = 0; i < frames.size(); i++) if (delays[i] <= 0) delays[i] = 100;
                if (frames.size() != delays.length) delays = java.util.Arrays.copyOf(delays, frames.size());
                return new Decoded(frames, delays, width, height);
            } finally {
                reader.dispose();
            }
        } catch (Throwable t) {
            E33Log.info("[e33chat] animated image decode failed ({}): {}", formatHint(bytes), t.toString());
            return null;
        }
    }

    private static String formatHint(byte[] bytes) {
        if (bytes == null || bytes.length < 6) return "unknown";
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') return "gif";
        if ((bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49
            && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46) return "webp";
        return "animated-image";
    }

    private static Decoded decodeGif(ImageReader reader, int count) throws Exception {
        int canvasW = Math.max(1, reader.getWidth(0));
        int canvasH = Math.max(1, reader.getHeight(0));
        IIOMetadata stream = reader.getStreamMetadata();
        if (stream != null) {
            try {
                var root = stream.getAsTree("javax_imageio_gif_stream_1.0");
                if (root instanceof IIOMetadataNode node) {
                    var descriptors = node.getElementsByTagName("LogicalScreenDescriptor");
                    if (descriptors.getLength() > 0 && descriptors.item(0) instanceof IIOMetadataNode d) {
                        canvasW = parseInt(d.getAttribute("logicalScreenWidth"), canvasW);
                        canvasH = parseInt(d.getAttribute("logicalScreenHeight"), canvasH);
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (canvasW > MAX_DIMENSION || canvasH > MAX_DIMENSION) return null;

        int budget = framesWithinBudget(canvasW, canvasH);
        if (budget < count) {
            E33Log.info("[e33chat] animated GIF trimmed to {} of {} frames (pixel budget)", budget, count);
            count = budget;
        }
        BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        ArrayList<NativeImage> frames = new ArrayList<>();
        int[] delays = new int[count];
        FrameInfo previous = null;
        BufferedImage restore = null;
        try {
            for (int i = 0; i < count; i++) {
                if (previous != null) {
                    if (previous.disposal() == 2) {
                        clear(canvas, previous.left(), previous.top(), previous.width(), previous.height());
                    } else if (previous.disposal() == 3 && restore != null) {
                        copyInto(restore, canvas);
                    }
                }
                IIOMetadata metadata = reader.getImageMetadata(i);
                FrameInfo info = frameInfo(reader, metadata, i);
                BufferedImage frame = reader.read(i);
                if (frame == null) break;
                BufferedImage before = info.disposal() == 3 ? copy(canvas) : null;
                Graphics2D graphics = canvas.createGraphics();
                try {
                    graphics.setComposite(AlphaComposite.SrcOver);

                    boolean logical = frame.getWidth() == canvasW && frame.getHeight() == canvasH
                        && (info.width() != canvasW || info.height() != canvasH);
                    graphics.drawImage(frame, logical ? 0 : info.left(), logical ? 0 : info.top(), null);
                } finally {
                    graphics.dispose();
                }
                frames.add(RasterImageDecoder.fromBufferedImage(canvas));
                delays[i] = info.delay();
                previous = info;
                restore = before;
            }
            if (frames.size() < 2) {
                closeFrames(frames);
                return null;
            }
            for (int i = 0; i < frames.size(); i++) if (delays[i] <= 0) delays[i] = 100;
            return new Decoded(frames, java.util.Arrays.copyOf(delays, frames.size()), canvasW, canvasH);
        } catch (Throwable t) {
            closeFrames(frames);
            throw t;
        }
    }

    private static FrameInfo frameInfo(ImageReader reader, IIOMetadata metadata, int index) throws Exception {
        int left = 0, top = 0, width = reader.getWidth(index), height = reader.getHeight(index), delay = 100, disposal = 0;
        if (metadata != null) {
            var root = metadata.getAsTree("javax_imageio_gif_image_1.0");
            if (root instanceof IIOMetadataNode node) {
                var descriptors = node.getElementsByTagName("ImageDescriptor");
                if (descriptors.getLength() > 0 && descriptors.item(0) instanceof IIOMetadataNode d) {
                    left = parseInt(d.getAttribute("imageLeftPosition"), 0);
                    top = parseInt(d.getAttribute("imageTopPosition"), 0);
                    width = parseInt(d.getAttribute("imageWidth"), width);
                    height = parseInt(d.getAttribute("imageHeight"), height);
                }
                var controls = node.getElementsByTagName("GraphicControlExtension");
                if (controls.getLength() > 0 && controls.item(0) instanceof IIOMetadataNode c) {
                    delay = Math.max(40, parseInt(c.getAttribute("delayTime"), 10) * 10);
                    String method = c.getAttribute("disposalMethod");
                    disposal = "restoreToBackgroundColor".equals(method) ? 2
                        : "restoreToPrevious".equals(method) ? 3 : 0;
                }
            }
        }
        return new FrameInfo(left, top, Math.max(1, width), Math.max(1, height), delay, disposal);
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copyInto(source, target);
        return target;
    }

    private static void copyInto(BufferedImage source, BufferedImage target) {
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
    }

    private static void clear(BufferedImage image, int x, int y, int width, int height) {
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(x, y, width, height);
        } finally {
            graphics.dispose();
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void closeFrames(ArrayList<NativeImage> frames) {
        for (NativeImage image : frames) {
            try { image.close(); } catch (Throwable ignored) {}
        }
        frames.clear();
    }

    private static int frameDelay(IIOMetadata metadata) {
        if (metadata == null) return 100;
        try {
            for (String format : metadata.getMetadataFormatNames()) {
                var root = metadata.getAsTree(format);
                if (!(root instanceof IIOMetadataNode node)) continue;
                var controls = node.getElementsByTagName("GraphicControlExtension");
                if (controls.getLength() > 0 && controls.item(0) instanceof IIOMetadataNode control) {
                    String value = control.getAttribute("delayTime");
                    if (!value.isBlank()) return Math.max(40, Integer.parseInt(value) * 10);
                }
            }
        } catch (Throwable ignored) {}
        return 100;
    }

    public static final class Entry {
        private final String url;
        private volatile Identifier[] frames;
        private volatile int[] delays;
        private volatile int width;
        private volatile int height;
        private volatile boolean ready;
        private volatile boolean failed;
        private volatile long failedAt;
        private volatile boolean staticImage;
        private volatile long sizeBytes;
        private volatile long frameStart;
        private volatile int frameIndex;

        private volatile long lastAccessMs = System.currentTimeMillis();

        private Entry(String url) {
            this.url = url;
        }

        public boolean ready() {
            return ready && frames != null && frames.length > 1;
        }

        public boolean failed() {
            return failed;
        }

        public boolean staticImage() {
            return staticImage;
        }

        public long sizeBytes() {
            return sizeBytes;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public Identifier texture() {
            lastAccessMs = System.currentTimeMillis();
            Identifier[] current = frames;
            if (current == null || current.length == 0) return null;
            advance(System.currentTimeMillis());
            return current[Math.max(0, Math.min(frameIndex, current.length - 1))];
        }

        private synchronized void advance(long now) {
            Identifier[] current = frames;
            int[] currentDelays = delays;
            if (!ready || current == null || current.length == 0
                || currentDelays == null || currentDelays.length != current.length) return;
            long elapsed = Math.max(0L, now - frameStart);
            while (elapsed >= Math.max(1, currentDelays[frameIndex])) {
                elapsed -= Math.max(1, currentDelays[frameIndex]);
                frameIndex = (frameIndex + 1) % current.length;
                frameStart = now - elapsed;
            }
        }

        private void markFailed() {
            failed = true;
            failedAt = System.currentTimeMillis();
        }
    }

    private record Decoded(ArrayList<NativeImage> frames, int[] delays, int width, int height) {}

    public record FrameTex(Identifier texture, int width, int height) {}

    private record FrameInfo(int left, int top, int width, int height, int delay, int disposal) {}
}
