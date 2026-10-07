package com.niuqu.chatbubble.mixin;

//#if MC >= 26000
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSuggestions.class)
public interface ChatInputSuggestorAccessor {
    @Accessor("suggestions")
    CommandSuggestions.SuggestionsList getWindow();
}
//#else
//#if MC >= 11900
//$$ import net.minecraft.client.gui.screen.ChatInputSuggestor;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ @Mixin(ChatInputSuggestor.class)
//$$ public interface ChatInputSuggestorAccessor {
//$$     @Accessor("window")
//$$     ChatInputSuggestor.SuggestionWindow getWindow();
//$$ }
//#else
//$$ public interface ChatInputSuggestorAccessor {
//$$ }
//#endif
//#endif
