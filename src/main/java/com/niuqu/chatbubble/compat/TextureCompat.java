package com.niuqu.chatbubble.compat;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

// 纹理创建版本适配
public final class TextureCompat {
    // 禁止外部实例化
    private TextureCompat() {}

    // 创建纹理对象
    public static NativeImageBackedTexture create(String name, NativeImage image) {
        //#if MC >= 12105
        return new NativeImageBackedTexture(() -> name, image);
        //#else
        //$$ return new NativeImageBackedTexture(image);
        //#endif
    }
}
