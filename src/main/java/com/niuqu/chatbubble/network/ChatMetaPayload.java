package com.niuqu.chatbubble.network;

import com.niuqu.chatbubble.store.ChatMessageStore;
import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//#if MC >= 12005
public record ChatMetaPayload(UUID senderUUID, String senderName, String messageHash,
                               String quoteSender, String quoteContent, List<String> mentionTargets)
        implements CustomPayload {
//#else
//$$ public record ChatMetaPayload(UUID senderUUID, String senderName, String messageHash,
//$$                                String quoteSender, String quoteContent, List<String> mentionTargets) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<ChatMetaPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "chat_meta")
            //#else
            //$$ new Identifier("e33chat", "chat_meta")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "chat_meta");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, ChatMetaPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeString(value.senderUUID.toString());
            buf.writeString(value.senderName);
            buf.writeString(value.messageHash);
            buf.writeString(value.quoteSender);
            buf.writeString(value.quoteContent);
            writeMentions(buf, value.mentionTargets);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeString(value.senderUUID.toString());
        //$$     buf.writeString(value.senderName);
        //$$     buf.writeString(value.messageHash);
        //$$     buf.writeString(value.quoteSender);
        //$$     buf.writeString(value.quoteContent);
        //$$     writeMentions(buf, value.mentionTargets);
        //$$ },
        //#endif
        buf -> new ChatMetaPayload(
            UUID.fromString(buf.readString()),
            buf.readString(),
            buf.readString(),
            buf.readString(),
            buf.readString(),
            readMentions(buf)
        )
    );
    //#else
    //$$ public static ChatMetaPayload read(PacketByteBuf buf) {
    //$$     return new ChatMetaPayload(
    //$$         UUID.fromString(buf.readString()),
    //$$         buf.readString(),
    //$$         buf.readString(),
    //$$         buf.readString(),
    //$$         buf.readString(),
    //$$         readMentions(buf)
    //$$     );
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeVarInt(mentionTargets.size());
    //$$     for (String s : mentionTargets) buf.writeString(s);
    //$$     buf.writeString(senderUUID.toString());
    //$$     buf.writeString(senderName);
    //$$     buf.writeString(messageHash);
    //$$     buf.writeString(quoteSender);
    //$$     buf.writeString(quoteContent);
    //$$     return buf;
    //$$ }
    //#endif

    /** Mention targets are a handful of names; the count comes off the wire, so
     *  an unclamped pre-allocation (vanilla readList/readCollection sizes the
     *  list from the wire count) lets one packet OOM the receiver.
     *  26.3 removed FriendlyByteBuf.writeCollection/readList — the helpers below
     *  write the identical wire format (VarInt count + elements) by hand. */
    private static final int MAX_MENTION_TARGETS = 200;

    private static void writeMentions(PacketByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeString(s);
    }

    private static List<String> readMentions(PacketByteBuf buf) {
        int count = Math.min(Math.max(buf.readVarInt(), 0), MAX_MENTION_TARGETS);
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(buf.readString());
        return out;
    }

    //#if MC >= 12005
    @Override
    public Id<ChatMetaPayload> getId() { return ID; }
    //#endif
}
