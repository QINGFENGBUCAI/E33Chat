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

/** Server -> client sync of server-side settings (v2: adds message-format templates). */
//#if MC >= 12005
public record ConfigSyncV2Payload(boolean useTpa, List<String> chatTemplates,
                                  List<String> whisperTemplates, boolean templateDebug)
        implements CustomPayload {
//#else
//$$ public record ConfigSyncV2Payload(boolean useTpa, List<String> chatTemplates,
//$$                                   List<String> whisperTemplates, boolean templateDebug) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<ConfigSyncV2Payload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "config_sync_v2")
            //#else
            //$$ new Identifier("e33chat", "config_sync_v2")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "config_sync_v2");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, ConfigSyncV2Payload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeBoolean(value.useTpa);
            writeList(buf, value.chatTemplates);
            writeList(buf, value.whisperTemplates);
            buf.writeBoolean(value.templateDebug);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeBoolean(value.useTpa);
        //$$     writeList(buf, value.chatTemplates);
        //$$     writeList(buf, value.whisperTemplates);
        //$$     buf.writeBoolean(value.templateDebug);
        //$$ },
        //#endif
        buf -> new ConfigSyncV2Payload(
            buf.readBoolean(),
            readList(buf),
            readList(buf),
            buf.readBoolean()
        )
    );
    //#else
    //$$ public static ConfigSyncV2Payload read(PacketByteBuf buf) {
    //$$     return new ConfigSyncV2Payload(
    //$$         buf.readBoolean(),
    //$$         readList(buf),
    //$$         readList(buf),
    //$$         buf.readBoolean()
    //$$     );
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeBoolean(useTpa);
    //$$     writeList(buf, chatTemplates);
    //$$     writeList(buf, whisperTemplates);
    //$$     buf.writeBoolean(templateDebug);
    //$$     return buf;
    //$$ }
    //#endif

    /** Template lists are a handful of entries; anything beyond the cap is a
     *  hostile or corrupt payload — the count comes off the wire, so an
     *  unclamped new ArrayList<>(count) lets one packet OOM the receiver. */
    static final int MAX_LIST_ENTRIES = 256;

    static List<String> readList(PacketByteBuf buf) {
        int count = Math.min(Math.max(buf.readInt(), 0), MAX_LIST_ENTRIES);
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(buf.readString());
        return out;
    }

    static void writeList(PacketByteBuf buf, List<String> list) {
        buf.writeInt(list.size());
        for (String s : list) buf.writeString(s);
    }

    //#if MC >= 12005
    @Override
    public Id<ConfigSyncV2Payload> getId() { return ID; }
    //#endif

    public static void handle(ConfigSyncV2Payload payload) {
        ChatMessageStore.setServerConfig(
            payload.useTpa(), payload.chatTemplates(), payload.whisperTemplates(), payload.templateDebug());
    }
}
