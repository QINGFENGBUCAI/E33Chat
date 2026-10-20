package com.niuqu.chatbubble.mixin;

//#if MC >= 11900
import com.niuqu.chatbubble.ChatBubbleScreen;
import net.minecraft.client.MinecraftClient;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatInputSuggestor.class, priority = 500)
public class ChatInputSuggestorMixin {
    //#if MC >= 26000
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"))
    private void onRenderMessages(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        repositionForChatBubble();
    }
    //#else
    //$$ @Inject(method = "renderMessages", at = @At("HEAD"))
    //$$ private void onRenderMessages(DrawContext context, CallbackInfo ci) {
    //$$     repositionForChatBubble();
    //$$ }
    //#endif
    private void repositionForChatBubble() {
        if (!(MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen)) return;
        //#if MC >= 26000
        net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList window =
            ((ChatInputSuggestorAccessor) this).getWindow();
        //#else
        //$$ ChatInputSuggestor.SuggestionWindow window = ((ChatInputSuggestorAccessor) this).getWindow();
        //#endif
        if (window == null) return;
        Rect2i area = ((SuggestionWindowAccessor) window).getArea();
        if (area == null) return;
        int newY = ChatBubbleScreen.getInputY() - area.getHeight() - 4;
        if (area.getY() != newY) area.setY(newY);
        if (area.getX() < ChatBubbleScreen.getInputX()) area.setX(ChatBubbleScreen.getInputX());
    }

    //#if MC >= 26000
    @Inject(method = "showSuggestions(Z)V", at = @At("TAIL"))
    private void afterShow(CallbackInfo ci) {
        repositionForChatBubble();
    }
    //#else
    //$$ @Inject(method = "show(Z)V", at = @At("TAIL"))
    //$$ private void afterShow(CallbackInfo ci) {
    //$$     repositionForChatBubble();
    //$$ }
    //#endif
}
//#else
//$$ public class ChatInputSuggestorMixin {
//$$ }
//#endif
