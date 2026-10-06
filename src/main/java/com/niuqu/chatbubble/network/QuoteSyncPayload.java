package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12005
public record QuoteSyncPayload(String quotedSenderName, String quotedContent, String messageHash)
        implements CustomPayload {
//#else
//$$ public record QuoteSyncPayload(String quotedSenderName, String quotedContent, String messageHash) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<QuoteSyncPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "quote_sync")
            //#else
            //$$ new Identifier("e33chat", "quote_sync")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "quote_sync");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, QuoteSyncPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeString(value.quotedSenderName);
            buf.writeString(value.quotedContent);
            buf.writeString(value.messageHash);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeString(value.quotedSenderName);
        //$$     buf.writeString(value.quotedContent);
        //$$     buf.writeString(value.messageHash);
        //$$ },
        //#endif
        buf -> new QuoteSyncPayload(buf.readString(), buf.readString(), buf.readString())
    );
    //#else
    //$$ public static QuoteSyncPayload read(PacketByteBuf buf) {
    //$$     return new QuoteSyncPayload(buf.readString(), buf.readString(), buf.readString());
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeString(quotedSenderName);
    //$$     buf.writeString(quotedContent);
    //$$     buf.writeString(messageHash);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public Id<QuoteSyncPayload> getId() { return ID; }
    //#endif
}
