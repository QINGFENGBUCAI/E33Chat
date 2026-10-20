package com.niuqu.chatbubble.mixin;

//#if MC >= 26000
import net.minecraft.client.multiplayer.chat.ChatListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatListener.class)
public interface MessageHandlerAccessor {
}
//#else
//#if MC >= 11900
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
