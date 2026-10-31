package com.niuqu.chatbubble.network;
import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;
import java.util.UUID;

// 群组聊天消息包
//#if MC >= 12005
public record GroupChatPayload(UUID senderUUID, String senderName, String groupName,
                               String content, String quoteSender, String quoteContent)
        implements CustomPayload {
//#else
//$$ public record GroupChatPayload(UUID senderUUID, String senderName, String groupName,
//$$                                String content, String quoteSender, String quoteContent) {
//#endif

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

    // 空串转空值
    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    // 获取包标识
    //#if MC >= 12005
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    //#endif

    // 客户端处理群聊
    public static void handleClient(GroupChatPayload payload) {
        com.niuqu.chatbubble.store.ChatMessageStore.addGroupMessage(
            net.minecraft.text.Text.literal(payload.content()),
            payload.senderUUID(),
            net.minecraft.text.Text.literal(payload.senderName()),
            payload.groupName(), payload.quoteSender(), payload.quoteContent());
    }
}
