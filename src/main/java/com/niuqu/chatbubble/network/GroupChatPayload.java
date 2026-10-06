package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * S2C group chat message, sent only to members running the mod (vanilla
 * members receive a plain formatted line instead). Carries the full content so
 * the client builds the bubble locally — no text-tag parsing, no echo.
 */
//#if MC >= 12005
public record GroupChatPayload(UUID senderUUID, String senderName, String groupName,
                               String content, String quoteSender, String quoteContent)
        implements CustomPayload {
//#else
//$$ public record GroupChatPayload(UUID senderUUID, String senderName, String groupName,
//$$                                String content, String quoteSender, String quoteContent) {
//#endif

    // Server caps content; the codec below caps reads via readString(max)
    private static final int MAX_TEXT = 2048;

    //#if MC >= 12005
    public static final CustomPayload.Id<GroupChatPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "group_chat")
            //#else
            //$$ new Identifier("e33chat", "group_chat")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "group_chat");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, GroupChatPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeUuid(value.senderUUID);
            buf.writeString(value.senderName, 256);
            buf.writeString(value.groupName, 64);
            buf.writeString(value.content, MAX_TEXT);
            buf.writeString(value.quoteSender != null ? value.quoteSender : "", 256);
            buf.writeString(value.quoteContent != null ? value.quoteContent : "", MAX_TEXT);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeUuid(value.senderUUID);
        //$$     buf.writeString(value.senderName, 256);
        //$$     buf.writeString(value.groupName, 64);
        //$$     buf.writeString(value.content, MAX_TEXT);
        //$$     buf.writeString(value.quoteSender != null ? value.quoteSender : "", 256);
        //$$     buf.writeString(value.quoteContent != null ? value.quoteContent : "", MAX_TEXT);
        //$$ },
        //#endif
        buf -> new GroupChatPayload(
            buf.readUuid(),
            buf.readString(256),
            buf.readString(64),
            buf.readString(MAX_TEXT),
            blankToNull(buf.readString(256)),
            blankToNull(buf.readString(MAX_TEXT))
        )
    );
    //#else
    //$$ public static GroupChatPayload read(PacketByteBuf buf) {
    //$$     return new GroupChatPayload(
    //$$         buf.readUuid(),
    //$$         buf.readString(256),
    //$$         buf.readString(64),
    //$$         buf.readString(MAX_TEXT),
    //$$         blankToNull(buf.readString(256)),
    //$$         blankToNull(buf.readString(MAX_TEXT))
    //$$     );
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeUuid(senderUUID);
    //$$     buf.writeString(senderName, 256);
    //$$     buf.writeString(groupName, 64);
    //$$     buf.writeString(content, MAX_TEXT);
    //$$     buf.writeString(quoteSender != null ? quoteSender : "", 256);
    //$$     buf.writeString(quoteContent != null ? quoteContent : "", MAX_TEXT);
    //$$     return buf;
    //$$ }
    //#endif

    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    //#if MC >= 12005
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    //#endif

    /** Client-side hook, invoked from ChatBubbleClientSetup's receiver. */
    public static void handleClient(GroupChatPayload payload) {
        com.niuqu.chatbubble.store.ChatMessageStore.addGroupMessage(
            net.minecraft.text.Text.literal(payload.content()),
            payload.senderUUID(),
            net.minecraft.text.Text.literal(payload.senderName()),
            payload.groupName(), payload.quoteSender(), payload.quoteContent());
    }
}
