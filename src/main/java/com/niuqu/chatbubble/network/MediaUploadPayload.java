package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12005
public record MediaUploadPayload(long uploadId, int index, int totalChunks,
                                 int totalBytes, String contentType, byte[] chunk)
        implements CustomPayload {
//#else
//$$ public record MediaUploadPayload(long uploadId, int index, int totalChunks,
//$$                                  int totalBytes, String contentType, byte[] chunk) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<MediaUploadPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "media_upload")
            //#else
            //$$ new Identifier("e33chat", "media_upload")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "media_upload");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, MediaUploadPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeLong(value.uploadId);
            buf.writeInt(value.index);
            buf.writeInt(value.totalChunks);
            buf.writeInt(value.totalBytes);
            buf.writeString(value.contentType != null ? value.contentType : "");
            buf.writeByteArray(value.chunk);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeLong(value.uploadId);
        //$$     buf.writeInt(value.index);
        //$$     buf.writeInt(value.totalChunks);
        //$$     buf.writeInt(value.totalBytes);
        //$$     buf.writeString(value.contentType != null ? value.contentType : "");
        //$$     buf.writeByteArray(value.chunk);
        //$$ },
        //#endif
        buf -> new MediaUploadPayload(
            buf.readLong(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readString(),
            buf.readByteArray()
        )
    );
    //#else
    //$$ public static MediaUploadPayload read(PacketByteBuf buf) {
    //$$     return new MediaUploadPayload(
    //$$         buf.readLong(),
    //$$         buf.readInt(),
    //$$         buf.readInt(),
    //$$         buf.readInt(),
    //$$         buf.readString(),
    //$$         buf.readByteArray()
    //$$     );
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeLong(uploadId);
    //$$     buf.writeInt(index);
    //$$     buf.writeInt(totalChunks);
    //$$     buf.writeInt(totalBytes);
    //$$     buf.writeString(contentType != null ? contentType : "");
    //$$     buf.writeByteArray(chunk);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public Id<MediaUploadPayload> getId() { return ID; }
    //#endif
}
