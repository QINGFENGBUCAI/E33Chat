package com.niuqu.chatbubble.compat;
import net.minecraft.text.Text;

// 文本序列化版本适配
public final class TextJsonCompat {
    // 禁止外部实例化
    private TextJsonCompat() {}

    // 序列化文本为字符串
    public static String toJson(Text text, Object registries) {
        if (text == null) return null;
        //#if MC >= 12106
        try {
            return net.minecraft.text.TextCodecs.CODEC
                .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, text)
                .getOrThrow().toString();
        } catch (Throwable t) {
            return null;
        }
        //#else
        //#if MC >= 12100
        //$$ try {
        //$$     return net.minecraft.text.Text.Serialization.toJsonString(
        //$$         text, (net.minecraft.registry.RegistryWrapper.WrapperLookup) registries);
        //$$ } catch (Throwable t) { return null; }
        //#else
        //$$ return null;
        //#endif
        //#endif
    }

    // 反序列化文本
    public static Text fromJson(String json, Object registries) {
        if (json == null) return null;
        //#if MC >= 12106
        try {
            return net.minecraft.text.TextCodecs.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(json))
                .getOrThrow();
        } catch (Throwable t) {
            return null;
        }
        //#else
        //#if MC >= 12100
        //$$ try {
        //$$     return net.minecraft.text.Text.Serialization.fromJson(
        //$$         json, (net.minecraft.registry.RegistryWrapper.WrapperLookup) registries);
        //$$ } catch (Throwable t) { return null; }
        //#else
        //$$ return null;
        //#endif
        //#endif
    }
}
