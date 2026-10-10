package com.niuqu.chatbubble.server;

import com.niuqu.chatbubble.network.MediaResponsePayload;
import com.niuqu.chatbubble.network.MediaUploadAckPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//#if MC < 12005
//$$ import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//#endif
import net.minecraft.server.network.ServerPlayerEntity;

public final class MediaService {
    private MediaService() {}

    public static void handleRequest(ServerPlayerEntity sender, DiskMediaStore store, String mediaId) {
        if (!store.allowTransfer(sender.getName().getString())) {
            sendNotFound(sender, mediaId);
            return;
        }
        long size = store.sizeOf(mediaId);
        if (size < 0) {
            sendNotFound(sender, mediaId);
            return;
        }
        int total = DiskMediaStore.totalChunksFor(size);
        for (int i = 0; i < total; i++) {
            byte[] chunk = store.readChunk(mediaId, i, total);
            if (chunk == null) {
                sendNotFound(sender, mediaId);
                return;
            }
            //#if MC >= 12005
            ServerPlayNetworking.send(sender, new MediaResponsePayload(mediaId, i, total, chunk));
            //#else
            //$$ MediaResponsePayload mr = new MediaResponsePayload(mediaId, i, total, chunk);
            //$$ ServerPlayNetworking.send(sender, MediaResponsePayload.ID, mr.write(PacketByteBufs.create()));
            //#endif
        }
    }

    private static void sendNotFound(ServerPlayerEntity sender, String mediaId) {
        //#if MC >= 12005
        ServerPlayNetworking.send(sender, new MediaResponsePayload(mediaId, 0, 1, new byte[0]));
        //#else
        //$$ MediaResponsePayload mr = new MediaResponsePayload(mediaId, 0, 1, new byte[0]);
        //$$ ServerPlayNetworking.send(sender, MediaResponsePayload.ID, mr.write(PacketByteBufs.create()));
        //#endif
    }

    public static void handleUpload(ServerPlayerEntity sender, DiskMediaStore store, boolean mediaEnabled,
                                    boolean autoClean, long uploadId, int index, int totalChunks,
                                    int totalBytes, String contentType, byte[] chunk) {
        if (!mediaEnabled) {
            //#if MC >= 12005
            ServerPlayNetworking.send(sender, new MediaUploadAckPayload(uploadId, null, "disabled"));
            //#else
            //$$ MediaUploadAckPayload ack = new MediaUploadAckPayload(uploadId, null, "disabled");
            //$$ ServerPlayNetworking.send(sender, MediaUploadAckPayload.ID, ack.write(PacketByteBufs.create()));
            //#endif
            return;
        }
        String result;
        if (index == 0) {
            if (!store.allowTransfer(sender.getName().getString())) {
                //#if MC >= 12005
                ServerPlayNetworking.send(sender, new MediaUploadAckPayload(uploadId, null, "rate limited"));
                //#else
                //$$ MediaUploadAckPayload ack = new MediaUploadAckPayload(uploadId, null, "rate limited");
                //$$ ServerPlayNetworking.send(sender, MediaUploadAckPayload.ID, ack.write(PacketByteBufs.create()));
                //#endif
                return;
            }
            result = store.beginUpload(uploadId, sender.getName().getString(),
                totalChunks, totalBytes, contentType);
            if (result == null) {
                result = store.acceptChunk(uploadId, index, chunk);
            }
        } else {
            result = store.acceptChunk(uploadId, index, chunk);
        }
        if (result == null) return;
        store.discardUpload(uploadId);
        if (DiskMediaStore.isValidMediaId(result) && autoClean)
            store.cleanupExpiredThrottled();
        //#if MC >= 12005
        ServerPlayNetworking.send(sender,
            DiskMediaStore.isValidMediaId(result)
                ? new MediaUploadAckPayload(uploadId, result, null)
                : new MediaUploadAckPayload(uploadId, null, result));
        //#else
        //$$ MediaUploadAckPayload ack = DiskMediaStore.isValidMediaId(result)
        //$$     ? new MediaUploadAckPayload(uploadId, result, null)
        //$$     : new MediaUploadAckPayload(uploadId, null, result);
        //$$ ServerPlayNetworking.send(sender, MediaUploadAckPayload.ID, ack.write(PacketByteBufs.create()));
        //#endif
    }
}
