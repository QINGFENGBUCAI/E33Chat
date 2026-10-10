package com.niuqu.chatbubble.mixin;

//#if MC >= 260100
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.ClientAsset;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.niuqu.chatbubble.HeadTextureHelper;

import java.util.function.Supplier;

@Mixin(SkinTextureDownloader.class)
public abstract class SkinTextureDownloaderMixin {
    @ModifyArg(
        method = "registerTextureInManager",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
        )
    )
    private static Supplier<?> e33chat$registerBlendedHeadTexture(
            Supplier<?> supplier,
            @Local(argsOnly = true) ClientAsset.Texture texture,
            @Local(argsOnly = true) NativeImage image
    ) {
        return () -> {
            Identifier textureLocation = texture.texturePath();
            HeadTextureHelper.registerBlendedHead(textureLocation, image);
            return supplier.get();
        };
    }
}
//#endif
