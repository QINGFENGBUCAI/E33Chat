package com.niuqu.chatbubble.mixin;

//#if MC >= 11900
import com.niuqu.chatbubble.ChatBubbleMod;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    //#if MC >= 26000
    @Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
        at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
    //$$     at = @At("HEAD"))
    //#endif
    private void onCommandExecuted(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        ChatBubbleMod.consumePrivateMessageQuote(parseResults, command);
    }
}
//#else
//$$ import net.minecraft.server.command.CommandManager;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$
//$$ @Mixin(CommandManager.class)
//$$ public class CommandManagerMixin {
//$$ }
//#endif
