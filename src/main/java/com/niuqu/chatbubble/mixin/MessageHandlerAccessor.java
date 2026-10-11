package com.niuqu.chatbubble.mixin;

//#if MC >= 26000

import net.minecraft.client.multiplayer.chat.ChatListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatListener.class)
public interface MessageHandlerAccessor {
}
//#else
//#if MC >= 11900
// MessageHandler.tryParseAsPlayerMessage does not exist on any target from
// 1.19.2 through 1.21.11 (verified per version), and nothing calls this
// accessor — keep the type registered as an empty mixin so the required
// config stays valid without a target member.
//$$ import net.minecraft.client.network.message.MessageHandler;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$
//$$ @Mixin(MessageHandler.class)
//$$ public interface MessageHandlerAccessor {
//$$ }
//#else
//$$ public interface MessageHandlerAccessor {
//$$ }
//#endif
//#endif
