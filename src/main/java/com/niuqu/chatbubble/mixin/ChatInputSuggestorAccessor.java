package com.niuqu.chatbubble.mixin;

//#if MC >= 26000
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 候选窗口读取访问器
@Mixin(CommandSuggestions.class)
public interface ChatInputSuggestorAccessor {
    // 获取候选窗口
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
