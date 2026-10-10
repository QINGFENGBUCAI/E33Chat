package com.niuqu.chatbubble.network;

import com.niuqu.chatbubble.image.MediaClient;
import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12005
public record MediaCapPayload(boolean mediaEnabled) implements CustomPayload {
//#else
//$$ public record MediaCapPayload(boolean mediaEnabled) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<MediaCapPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "media_cap")
            //#else
            //$$ new Identifier("e33chat", "media_cap")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "media_cap");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, MediaCapPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> buf.writeBoolean(value.mediaEnabled),
        //#else
        //$$ (value, buf) -> buf.writeBoolean(value.mediaEnabled),
        //#endif
        buf -> new MediaCapPayload(buf.readBoolean())
    );
    //#else
    //$$ public static MediaCapPayload read(PacketByteBuf buf) {
    //$$     return new MediaCapPayload(buf.readBoolean());
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeBoolean(mediaEnabled);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public Id<MediaCapPayload> getId() { return ID; }
    //#endif

    public static void handle(MediaCapPayload payload) {
        MediaClient.setServerEnabled(payload.mediaEnabled());
    }
}
