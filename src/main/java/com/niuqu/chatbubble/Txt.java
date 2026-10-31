package com.niuqu.chatbubble;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
//#if MC < 11900
//$$ import net.minecraft.text.LiteralText;
//$$ import net.minecraft.text.TranslatableText;
//#endif
// 文本对象构建工具
public final class Txt {
    // 禁止外部实例化
    private Txt() {}

    // 创建字面文本
    public static MutableText literal(String text) {
        //#if MC >= 11900
        return Text.literal(text);
        //#else
        //$$ return new LiteralText(text);
        //#endif
    }

    // 创建翻译文本
    public static MutableText translatable(String key, Object... args) {
        //#if MC >= 11900
        return Text.translatable(key, args);
        //#else
        //$$ return new TranslatableText(key, args);
        //#endif
    }

    // 创建空文本
    public static MutableText empty() {
        //#if MC >= 11900
        return Text.empty();
        //#else
        //$$ return new LiteralText("");
        //#endif
    }

}
