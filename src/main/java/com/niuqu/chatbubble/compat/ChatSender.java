package com.niuqu.chatbubble.compat;

import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Client-side chat/command sending across the 1.19 chat rework.
 *
 * {@code ClientPlayNetworkHandler.sendChatMessage/sendChatCommand} only appeared in
 * 1.19.3. Before that the entry points lived on {@code ClientPlayerEntity} and the
 * signatures differ again between the pre-1.19 and 1.19.0-1.19.2 eras, so the whole
 * dispatch is centralised here instead of being regex-rewritten per call site.
 */
public final class ChatSender {
    private ChatSender() {}

    public static void sendCommand(ClientPlayerEntity player, String command) {
        if (player == null || command == null) return;
        //#if MC >= 11903
        player.networkHandler.sendChatCommand(command);
        //#else
        //#if MC >= 11900
        //$$ player.sendCommand(command);
        //#else
        //$$ player.sendChatMessage("/" + command);
        //#endif
        //#endif
    }

    public static void sendChat(ClientPlayerEntity player, String message) {
        if (player == null || message == null) return;
        //#if MC >= 11903
        player.networkHandler.sendChatMessage(message);
        //#else
        //#if MC >= 11900
        //$$ player.sendChatMessage(message, null);
        //#else
        //$$ player.sendChatMessage(message);
        //#endif
        //#endif
    }
}
