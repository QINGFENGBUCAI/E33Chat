package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * C2S handshake: the client announces it runs E33Chat right after logging in.
 * The server uses this to route group chat (packet for mod clients, plain
 * formatted line for vanilla ones) and to push the group list immediately.
 */
//#if MC >= 12005
public record ClientHelloPayload() implements CustomPayload {
//#else
//$$ public record ClientHelloPayload() {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<ClientHelloPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "client_hello")
            //#else
            //$$ new Identifier("e33chat", "client_hello")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "client_hello");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, ClientHelloPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> {},
        //#else
        //$$ (value, buf) -> {},
        //#endif
        buf -> new ClientHelloPayload()
    );
    //#else
    //$$ public static ClientHelloPayload read(PacketByteBuf buf) {
    //$$     return new ClientHelloPayload();
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    //#endif

    /**
     * Client-side dispatch. Only sent when the server actually negotiated the
     * e33chat channel: on a vanilla server the payload has no peer, and a failed
     * send inside the login flow must degrade to a warning, never cost the rest
     * of the login (the join event fires mid-{@code handleLogin}).
     */
    public static void send() {
        //#if MC >= 12005
        if (!net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(ID)) {
            com.niuqu.chatbubble.E33Log.info("[e33chat] Server has no e33chat channel; skipping client hello");
            return;
        }
        try {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new ClientHelloPayload());
        } catch (RuntimeException t) {
            com.niuqu.chatbubble.E33Log.warn("[e33chat] client hello send failed (login continues)", t);
        }
        //#else
        //$$ if (!net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(ID)) {
        //$$     com.niuqu.chatbubble.E33Log.info("[e33chat] Server has no e33chat channel; skipping client hello");
        //$$     return;
        //$$ }
        //$$ try {
        //$$     net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
        //$$         ID, new PacketByteBuf(io.netty.buffer.Unpooled.buffer()));
        //$$ } catch (RuntimeException t) {
        //$$     com.niuqu.chatbubble.E33Log.warn("[e33chat] client hello send failed (login continues)", t);
        //$$ }
        //#endif
    }

    /** Server-side hook, invoked from ChatBubbleMod's receiver. */
    public static void handleServer(ClientHelloPayload payload, ServerPlayerEntity player) {
        com.niuqu.chatbubble.server.GroupManager.onClientHello(player);
    }
}
