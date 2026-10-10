package com.niuqu.chatbubble.compat;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

public final class TextureCompat {
    private TextureCompat() {}

    public static NativeImageBackedTexture create(String name, NativeImage image) {
        //#if MC >= 12105
        return new NativeImageBackedTexture(() -> name, image);
        //#else
        //$$ return new NativeImageBackedTexture(image);
        //#endif
    }
}
