package com.niuqu.chatbubble.network;
import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 聊天历史同步包
//#if MC >= 12005
public record HistoryPayload(List<HistoryPayload.HistoryEntry> entries)
        implements CustomPayload {
//#else
//$$ public record HistoryPayload(List<HistoryPayload.HistoryEntry> entries) {
//#endif
    //#if MC >= 12005
    public static final CustomPayload.Id<HistoryPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "chat_history")
            //#else
            //$$ new Identifier("e33chat", "chat_history")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "chat_history");
    //#endif

    // 历史消息行
    public record HistoryEntry(
        UUID senderUUID,
        String senderName,
        String content,
        long time,
        boolean isSystem,
        String replyContent,
        String replySender,
        String group
    ) {}

    private static final UUID NULL_UUID = new UUID(0, 0);

    private static final int MAX_ENTRIES = 200;

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, HistoryPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> writeAll(buf, value.entries),
        //#else
        //$$ (value, buf) -> writeAll(buf, value.entries),
        //#endif
        buf -> {
            int count = Math.min(Math.max(buf.readVarInt(), 0), MAX_ENTRIES);
            List<HistoryEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) entries.add(new HistoryEntry(
                UUID.fromString(buf.readString()),
                buf.readString(),
                buf.readString(),
                buf.readLong(),
                buf.readBoolean(),
                nullOrEmpty(buf.readString()),
                nullOrEmpty(buf.readString()),
                nullOrEmpty(buf.readString())
            ));
            return new HistoryPayload(entries);
        }
    );
    //#else
    //$$ public static HistoryPayload read(PacketByteBuf buf) {
    //$$     int count = Math.min(Math.max(buf.readVarInt(), 0), MAX_ENTRIES);
    //$$     java.util.List<HistoryEntry> list = new java.util.ArrayList<>();
    //$$     for (int i = 0; i < count; i++) {
    //$$         list.add(new HistoryEntry(
    //$$             UUID.fromString(buf.readString()),
    //$$             buf.readString(),
    //$$             buf.readString(),
    //$$             buf.readLong(),
    //$$             buf.readBoolean(),
    //$$             nullOrEmpty(buf.readString()),
    //$$             nullOrEmpty(buf.readString()),
    //$$             nullOrEmpty(buf.readString())
    //$$         ));
    //$$     }
    //$$     return new HistoryPayload(list);
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     return writeAll(buf, entries);
    //$$ }
    //#endif

    // 写入全部历史
    private static PacketByteBuf writeAll(PacketByteBuf buf, List<HistoryEntry> entries) {
        List<HistoryEntry> rows = new ArrayList<>();
        if (entries != null) {
            for (HistoryEntry e : entries) {
                if (e != null) rows.add(e);
            }
            if (rows.size() < entries.size()) {
                com.niuqu.chatbubble.E33Log.warn("[e33chat] History packet: dropped {} null row(s) of {}",
                    entries.size() - rows.size(), entries.size());
            }
        }
        buf.writeVarInt(rows.size());
        for (HistoryEntry e : rows) writeEntry(buf, e);
        return buf;
    }

    // 写入单条历史
    private static void writeEntry(PacketByteBuf buf, HistoryEntry e) {
        buf.writeString((e.senderUUID() != null ? e.senderUUID() : NULL_UUID).toString());
        buf.writeString(e.senderName() != null ? e.senderName() : "");
        buf.writeString(e.content() != null ? e.content() : "");
        buf.writeLong(e.time());
        buf.writeBoolean(e.isSystem());
        buf.writeString(e.replyContent() != null ? e.replyContent() : "");
        buf.writeString(e.replySender() != null ? e.replySender() : "");
        buf.writeString(e.group() != null ? e.group() : "");
    }

    // 空串转空值
    private static String nullOrEmpty(String s) { return s == null || s.isEmpty() ? null : s; }

    // 获取包标识
    //#if MC >= 12005
    @Override
    public Id<HistoryPayload> getId() { return ID; }
    //#endif
}
