package com.niuqu.chatbubble.mixin;

//#if MC >= 26000
// 26.x: ChatListener no longer has tryParseAsPlayerMessage, and nothing in the
// mod calls this accessor anymore — keep the type registered but empty so the
// mixin config stays valid without a target member.
import net.minecraft.client.multiplayer.chat.ChatListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatListener.class)
public interface MessageHandlerAccessor {
}
//#else
//#if MC >= 11900
import com.niuqu.chatbubble.store.ChatMessageStore.SenderMeta;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MessageHandler.class)
public interface MessageHandlerAccessor {
    @Invoker(value = "tryParseAsPlayerMessage", remap = false)
    static SenderMeta e33chat$invokeTryParseAsPlayerMessage(Text message, String text) {
        throw new AssertionError();
    }
}
//#else
//$$ public interface MessageHandlerAccessor {
//$$ }
//#endif
//#endif
