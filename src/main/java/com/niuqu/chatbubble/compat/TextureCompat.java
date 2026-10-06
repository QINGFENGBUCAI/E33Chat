package com.niuqu.chatbubble.compat;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Dynamic texture construction. 1.21.5 replaced the {@code (NativeImage)}
 * constructor with a {@code (Supplier<String>, NativeImage)} pair (the supplier
 * names the texture for debug dumps).
 */
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
