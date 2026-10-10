package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12005
public record GroupActionPayload(int action, String groupName) implements CustomPayload {
//#else
//$$ public record GroupActionPayload(int action, String groupName) {
//#endif

    public static final int CREATE = 0;
    public static final int JOIN = 1;
    public static final int LEAVE = 2;
    public static final int DELETE = 3;

    private static final int MAX_NAME = 64;

    //#if MC >= 12005
    public static final CustomPayload.Id<GroupActionPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "group_action")
            //#else
            //$$ new Identifier("e33chat", "group_action")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "group_action");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, GroupActionPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {
            buf.writeVarInt(value.action);
            buf.writeString(value.groupName, MAX_NAME);
        },
        //#else
        //$$ (value, buf) -> {
        //$$     buf.writeVarInt(value.action);
        //$$     buf.writeString(value.groupName, MAX_NAME);
        //$$ },
        //#endif
        buf -> new GroupActionPayload(buf.readVarInt(), buf.readString(MAX_NAME))
    );
    //#else
    //$$ public static GroupActionPayload read(PacketByteBuf buf) {
    //$$     return new GroupActionPayload(buf.readVarInt(), buf.readString(MAX_NAME));
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeVarInt(action);
    //$$     buf.writeString(groupName, MAX_NAME);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    //#endif

    public static void send(int action, String groupName) {
        //#if MC >= 12005
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
            new GroupActionPayload(action, groupName));
        //#else
        //$$ GroupActionPayload p = new GroupActionPayload(action, groupName);
        //$$ PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        //$$ p.write(buf);
        //$$ net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(ID, buf);
        //#endif
    }
}
