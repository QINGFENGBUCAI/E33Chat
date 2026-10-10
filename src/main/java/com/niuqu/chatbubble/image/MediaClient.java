package com.niuqu.chatbubble.image;

import com.niuqu.chatbubble.E33Log;
import com.niuqu.chatbubble.network.MediaRequestPayload;
import com.niuqu.chatbubble.network.MediaResponsePayload;
import com.niuqu.chatbubble.network.MediaUploadAckPayload;
import com.niuqu.chatbubble.network.MediaUploadPayload;
import com.niuqu.chatbubble.server.DiskMediaStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#if MC < 12005
//$$ import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//#endif
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class MediaClient {
    private static final long TIMEOUT_SECONDS = 30;

    private static volatile boolean serverEnabled;
    private static final Map<Long, CompletableFuture<String>> UPLOADS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<byte[]>> FETCHES = new ConcurrentHashMap<>();
    private static final Map<String, byte[][]> FETCH_BUFFERS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FETCH_COUNTS = new ConcurrentHashMap<>();

    private static final int OWN_UPLOAD_CACHE_ENTRIES = 24;
    private static final Map<String, byte[]> OWN_UPLOADS = new ConcurrentHashMap<>();
    private static final java.util.Deque<String> OWN_UPLOAD_ORDER = new java.util.ArrayDeque<>();

    private static void rememberOwnUpload(String mediaId, byte[] bytes) {
        if (mediaId == null || bytes == null || bytes.length == 0) return;
        synchronized (OWN_UPLOAD_ORDER) {
            OWN_UPLOADS.put(mediaId, bytes);
            OWN_UPLOAD_ORDER.remove(mediaId);
            OWN_UPLOAD_ORDER.addLast(mediaId);
            while (OWN_UPLOAD_ORDER.size() > OWN_UPLOAD_CACHE_ENTRIES) {
                OWN_UPLOADS.remove(OWN_UPLOAD_ORDER.removeFirst());
            }
        }
    }

    private MediaClient() {}

    public static void setServerEnabled(boolean b) { serverEnabled = b; }
    public static boolean serverEnabled() { return serverEnabled; }

    public static void registerReceivers() {
        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(MediaUploadAckPayload.ID, (payload, context) -> {
            CompletableFuture<String> f = UPLOADS.remove(payload.uploadId());
            if (f != null) {
                f.complete(payload.error() == null ? payload.mediaId() : null);
            }
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(MediaUploadAckPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     MediaUploadAckPayload payload = MediaUploadAckPayload.read(buf);
        //$$     CompletableFuture<String> f = UPLOADS.remove(payload.uploadId());
        //$$     if (f != null) {
        //$$         f.complete(payload.error() == null ? payload.mediaId() : null);
        //$$     }
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(MediaResponsePayload.ID, (payload, context) ->
            handleResponse(payload));
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(MediaResponsePayload.ID, (client, handler, buf, responseSender) ->
        //$$     handleResponse(MediaResponsePayload.read(buf)));
        //#endif
    }

    private static void failFetch(String id, String message) {
        FETCH_BUFFERS.remove(id);
        FETCH_COUNTS.remove(id);
        CompletableFuture<byte[]> f = FETCHES.remove(id);
        if (f != null) f.completeExceptionally(new RuntimeException(message));
    }

    public static void handleResponse(MediaResponsePayload payload) {
        String id = payload.mediaId();
        if (payload.totalChunks() == 1 && payload.chunk().length == 0) {

            failFetch(id, "media not found: " + id);
            return;
        }

        int totalChunks = payload.totalChunks();
        if (!com.niuqu.chatbubble.server.DiskMediaStore.isValidChunkCount(totalChunks)) {
            failFetch(id, "media chunk count out of range: " + id);
            return;
        }
        byte[][] buf = FETCH_BUFFERS.computeIfAbsent(id, k -> new byte[totalChunks][]);
        if (payload.index() < 0 || payload.index() >= buf.length) return;
        buf[payload.index()] = payload.chunk();
        int got = FETCH_COUNTS.merge(id, 1, Integer::sum);
        if (got == totalChunks) {
            FETCH_BUFFERS.remove(id);
            FETCH_COUNTS.remove(id);
            CompletableFuture<byte[]> f = FETCHES.remove(id);
            if (f != null) {
                int total = 0;
                boolean complete = true;
                for (byte[] c : buf) {
                    if (c == null) { complete = false; break; }
                    total += c.length;
                }
                if (!complete) {
                    f.completeExceptionally(new RuntimeException("media chunk missing: " + id));
                } else if (total > com.niuqu.chatbubble.server.DiskMediaStore.MAX_SINGLE_BYTES) {
                    f.completeExceptionally(new RuntimeException("media too large: " + id));
                } else {
                    byte[] all = new byte[total];
                    int off = 0;
                    for (byte[] c : buf) {
                        System.arraycopy(c, 0, all, off, c.length);
                        off += c.length;
                    }
                    f.complete(all);
                }
            }
        }
    }

    public static String upload(byte[] bytes, String contentType) {
        if (!serverEnabled || bytes == null || bytes.length == 0) return null;
        if (!ClientPlayNetworking.canSend(MediaUploadPayload.ID)) return null;
        long uploadId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        int totalChunks = DiskMediaStore.totalChunksFor(bytes.length);
        CompletableFuture<String> done = new CompletableFuture<>();
        UPLOADS.put(uploadId, done);
        for (int i = 0; i < totalChunks; i++) {
            int from = i * DiskMediaStore.CHUNK_BYTES;
            int len = Math.min(DiskMediaStore.CHUNK_BYTES, bytes.length - from);
            byte[] chunk = new byte[len];
            System.arraycopy(bytes, from, chunk, 0, len);
            final int idx = i;
            MinecraftClient.getInstance().execute(() -> {
                try {
                    //#if MC >= 12005
                    ClientPlayNetworking.send(new MediaUploadPayload(uploadId, idx, totalChunks,
                        bytes.length, contentType, chunk));
                    //#else
                    //$$ MediaUploadPayload p = new MediaUploadPayload(uploadId, idx, totalChunks,
                    //$$     bytes.length, contentType, chunk);
                    //$$ ClientPlayNetworking.send(MediaUploadPayload.ID, p.write(PacketByteBufs.create()));
                    //#endif
                } catch (Throwable t) {
                    UPLOADS.remove(uploadId);
                    done.complete(null);
                }
            });
        }
        try {
            String mediaId = done.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (mediaId != null) rememberOwnUpload(mediaId, bytes);
            return mediaId != null ? "e33chat://media/" + mediaId : null;
        } catch (Exception e) {
            UPLOADS.remove(uploadId);
            E33Log.info("[e33chat] server media upload timed out after {}s", TIMEOUT_SECONDS);
            return null;
        }
    }

    public static byte[] fetch(String mediaId) {
        if (!DiskMediaStore.isValidMediaId(mediaId)) return null;
        if (!ClientPlayNetworking.canSend(MediaRequestPayload.ID)) return null;

        byte[] own = OWN_UPLOADS.get(mediaId);
        if (own != null) return own;

        CompletableFuture<byte[]> done = FETCHES.computeIfAbsent(mediaId, id -> {
            CompletableFuture<byte[]> fresh = new CompletableFuture<>();
            MinecraftClient.getInstance().execute(() -> {
                try {
                    //#if MC >= 12005
                    ClientPlayNetworking.send(new MediaRequestPayload(id));
                    //#else
                    //$$ MediaRequestPayload p = new MediaRequestPayload(id);
                    //$$ ClientPlayNetworking.send(MediaRequestPayload.ID, p.write(PacketByteBufs.create()));
                    //#endif
                } catch (Throwable t) {
                    FETCHES.remove(id, fresh);
                    fresh.completeExceptionally(t);
                }
            });
            return fresh;
        });
        try {
            return done.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            FETCHES.remove(mediaId, done);
            FETCH_BUFFERS.remove(mediaId);
            FETCH_COUNTS.remove(mediaId);
            E33Log.info("[e33chat] server media fetch {} timed out after {}s", mediaId, TIMEOUT_SECONDS);
            return null;
        } catch (Exception e) {

            FETCHES.remove(mediaId, done);
            FETCH_BUFFERS.remove(mediaId);
            FETCH_COUNTS.remove(mediaId);
            E33Log.info("[e33chat] server media fetch {} refused: {} (most likely the per-player "
                + "transfer rate limit)", mediaId, e.getMessage());
            return null;
        }
    }
}
