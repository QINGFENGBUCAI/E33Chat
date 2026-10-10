package com.niuqu.chatbubble.mixin;

import com.niuqu.chatbubble.render.HudVisibility;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
//#if MC >= 260200

import net.minecraft.client.gui.Hud;
//#else
//#if MC >= 26000
import net.minecraft.client.gui.Gui;
//#else
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
//#endif
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260200
@Mixin(Hud.class)
//#else
//#if MC >= 26000
//$$ @Mixin(Gui.class)
//#else
//$$ @Mixin(InGameHud.class)
//#endif
//#endif
public class InGameHudMixin {
    //#if MC >= 26000

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"), cancellable = true)
    private void e33chat$hideHudForTranslucentScreens(CallbackInfo ci) {
        if (HudVisibility.shouldHideHud()) ci.cancel();
    }
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //$$ private void e33chat$hideHudForTranslucentScreens(DrawContext context, RenderTickCounter tickCounter,
    //$$                                                   CallbackInfo ci) {
    //$$     if (HudVisibility.shouldHideHud()) ci.cancel();
    //$$ }
    //#endif
}
