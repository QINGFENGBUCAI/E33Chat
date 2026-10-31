package com.niuqu.chatbubble.compat;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

// 文本样式版本适配
public final class StyleCompat {
    // 禁止外部实例化
    private StyleCompat() {}

    // 提取点击事件值
    public static String clickValue(ClickEvent click) {
        if (click == null) return null;
        //#if MC >= 12105
        if (click instanceof ClickEvent.OpenUrl c) return c.uri().toString();
        if (click instanceof ClickEvent.RunCommand c) return c.command();
        if (click instanceof ClickEvent.SuggestCommand c) return c.command();
        if (click instanceof ClickEvent.CopyToClipboard c) return c.value();
        if (click instanceof ClickEvent.ChangePage c) return String.valueOf(c.page());
        return null;
        //#else
        //$$ return click.getValue();
        //#endif
    }

    // 创建打开链接事件
    public static ClickEvent openUrl(String url) {
        if (url == null) return null;
        //#if MC >= 12105
        try {
            return new ClickEvent.OpenUrl(new java.net.URI(url));
        } catch (Exception e) {
            return null;
        }
        //#else
        //$$ return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        //#endif
    }

    // 创建文本悬停事件
    public static HoverEvent showText(Text text) {
        if (text == null) return null;
        //#if MC >= 12105
        return new HoverEvent.ShowText(text);
        //#else
        //$$ return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
        //#endif
    }

    // 提取悬停文本
    public static Text hoverShowText(HoverEvent hover) {
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT) return null;
        //#if MC >= 12105
        return hover instanceof HoverEvent.ShowText st ? st.value() : null;
        //#else
        //$$ Object v = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        //$$ return v instanceof Text ? (Text) v : null;
        //#endif
    }

    // 提取悬停事件值
    public static Object hoverValue(HoverEvent hover) {
        if (hover == null) return null;
        //#if MC >= 12105
        if (hover instanceof HoverEvent.ShowText st) return st.value();
        if (hover instanceof HoverEvent.ShowItem si) return si.item();
        try {
            java.lang.reflect.Method m = hover.getClass().getMethod("value");
            return m.invoke(hover);
        } catch (Throwable t) {
            return null;
        }
        //#else
        //$$ try {
        //$$     return hover.getValue(hover.getAction());
        //$$ } catch (Throwable t) {
        //$$     return null;
        //$$ }
        //#endif
    }
}
