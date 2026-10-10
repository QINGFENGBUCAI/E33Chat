package com.niuqu.chatbubble.compat;

import net.minecraft.client.network.ClientPlayerEntity;

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
