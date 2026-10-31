package com.niuqu.chatbubble.compat;
import net.minecraft.client.network.ClientPlayerEntity;

// 聊天消息发送封装
public final class ChatSender {
    // 禁止外部实例化
    private ChatSender() {}

    // 发送命令消息
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

    // 发送聊天消息
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
