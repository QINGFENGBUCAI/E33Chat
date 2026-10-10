package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12005
public record MediaResponsePayload(String mediaId, int index, int totalChunks, byte[] chunk)
        implements CustomPayload {
//#else
//$$ public record MediaResponsePayload(String mediaId, int index, int totalChunks, byte[] chunk) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<MediaResponsePayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "media_response")
            //#else
            //$$ new Identifier("e33chat", "media_response")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "media_response");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, MediaResponsePayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeString(value.mediaId);
            buf.writeInt(value.index);
            buf.writeInt(value.totalChunks);
            buf.writeByteArray(value.chunk);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeString(value.mediaId);
        //$$     buf.writeInt(value.index);
        //$$     buf.writeInt(value.totalChunks);
        //$$     buf.writeByteArray(value.chunk);
        //$$ },
        //#endif
        buf -> new MediaResponsePayload(
            buf.readString(),
            buf.readInt(),
            buf.readInt(),
            buf.readByteArray()
        )
    );
    //#else
    //$$ public static MediaResponsePayload read(PacketByteBuf buf) {
    //$$     return new MediaResponsePayload(
    //$$         buf.readString(),
    //$$         buf.readInt(),
    //$$         buf.readInt(),
    //$$         buf.readByteArray()
    //$$     );
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeString(mediaId);
    //$$     buf.writeInt(index);
    //$$     buf.writeInt(totalChunks);
    //$$     buf.writeByteArray(chunk);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public Id<MediaResponsePayload> getId() { return ID; }
    //#endif
}
