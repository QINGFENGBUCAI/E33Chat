package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

import java.util.List;

//#if MC >= 12005
public record GroupListPayload(boolean enabled, List<String> names,
                               List<Integer> memberCounts, List<String> myGroups)
        implements CustomPayload {
//#else
//$$ public record GroupListPayload(boolean enabled, List<String> names,
//$$                                List<Integer> memberCounts, List<String> myGroups) {
//#endif

    private static final int MAX_GROUPS = 200;

    //#if MC >= 12005
    public static final CustomPayload.Id<GroupListPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "group_list")
            //#else
            //$$ new Identifier("e33chat", "group_list")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "group_list");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, GroupListPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeBoolean(value.enabled);
            writeStringList(buf, value.names, 64);
            writeVarIntList(buf, value.memberCounts);
            writeStringList(buf, value.myGroups, 64);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeBoolean(value.enabled);
        //$$     writeStringList(buf, value.names, 64);
        //$$     writeVarIntList(buf, value.memberCounts);
        //$$     writeStringList(buf, value.myGroups, 64);
        //$$ },
        //#endif
        buf -> new GroupListPayload(
            buf.readBoolean(),
            cap(readStringList(buf, 64)),
            cap(readVarIntList(buf)),
            cap(readStringList(buf, 64))
        )
    );
    //#else
    //$$ public static GroupListPayload read(PacketByteBuf buf) {
    //$$     buf.readBoolean();
    //$$     int n1 = buf.readVarInt();
    //$$     java.util.List<String> rNames = new java.util.ArrayList<>();
    //$$     for (int i = 0; i < n1; i++) rNames.add(buf.readString(64));
    //$$     int n2 = buf.readVarInt();
    //$$     java.util.List<Integer> rCounts = new java.util.ArrayList<>();
    //$$     for (int i = 0; i < n2; i++) rCounts.add(buf.readVarInt());
    //$$     int n3 = buf.readVarInt();
    //$$     java.util.List<String> rMyGroups = new java.util.ArrayList<>();
    //$$     for (int i = 0; i < n3; i++) rMyGroups.add(buf.readString(64));
    //$$     return new GroupListPayload(true, cap(rNames), cap(rCounts), cap(rMyGroups));
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeBoolean(enabled);
    //$$     buf.writeVarInt(names.size());
    //$$     for (String s : names) buf.writeString(s, 64);
    //$$     buf.writeVarInt(memberCounts.size());
    //$$     for (int c : memberCounts) buf.writeVarInt(c);
    //$$     buf.writeVarInt(myGroups.size());
    //$$     for (String s : myGroups) buf.writeString(s, 64);
    //$$     return buf;
    //$$ }
    //#endif

    private static void writeStringList(PacketByteBuf buf, List<String> list, int maxLen) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeString(s, maxLen);
    }

    private static List<String> readStringList(PacketByteBuf buf, int maxLen) {
        int n = buf.readVarInt();
        List<String> out = new java.util.ArrayList<>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) out.add(buf.readString(maxLen));
        return out;
    }

    private static void writeVarIntList(PacketByteBuf buf, List<Integer> list) {
        buf.writeVarInt(list.size());
        for (int v : list) buf.writeVarInt(v);
    }

    private static List<Integer> readVarIntList(PacketByteBuf buf) {
        int n = buf.readVarInt();
        List<Integer> out = new java.util.ArrayList<>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) out.add(buf.readVarInt());
        return out;
    }

    private static <T> List<T> cap(List<T> list) {
        return list.size() > MAX_GROUPS ? list.subList(0, MAX_GROUPS) : list;
    }

    //#if MC >= 12005
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    //#endif

    public static void handleClient(GroupListPayload payload) {
        com.niuqu.chatbubble.chat.GroupChannelState.enabled = payload.enabled();
        com.niuqu.chatbubble.chat.GroupChannelState.applyDirectory(
            payload.names(), payload.memberCounts(), payload.myGroups());
    }
}
