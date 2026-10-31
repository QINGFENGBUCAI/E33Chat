package com.niuqu.chatbubble.mixin;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// AWT 无头模式修复
@Mixin(Main.class)
public class AwtHeadlessHackMixin {
    // 关闭 AWT 无头模式
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void e33chat$allowAwt(CallbackInfo ci) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac")) {
            System.setProperty("java.awt.headless", "false");
        }
    }
}
