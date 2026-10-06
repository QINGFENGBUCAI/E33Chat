package com.niuqu.chatbubble.compat;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

/**
 * ClickEvent/HoverEvent bridge.
 *
 * 1.21.5 turned both into sealed interfaces whose payloads live in per-action
 * records, dropping the {@code getValue(...)} accessors the older class-based
 * API exposed. Centralising the dispatch here keeps the call sites readable.
 */
public final class StyleCompat {
    private StyleCompat() {}

    /** Payload of a click event as a string (URL, command, file path, page). */
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

    public static HoverEvent showText(Text text) {
        if (text == null) return null;
        //#if MC >= 12105
        return new HoverEvent.ShowText(text);
        //#else
        //$$ return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
        //#endif
    }

    /** Text payload of a SHOW_TEXT hover event; null for every other action. */
    public static Text hoverShowText(HoverEvent hover) {
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT) return null;
        //#if MC >= 12105
        return hover instanceof HoverEvent.ShowText st ? st.value() : null;
        //#else
        //$$ Object v = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        //$$ return v instanceof Text ? (Text) v : null;
        //#endif
    }

    /**
     * Best-effort payload of an arbitrary hover action. ChatImage and similar
     * mods ship their own HoverEvent implementation; on 1.21.5+ the payload is
     * exposed through a {@code value()} accessor, reached reflectively.
     */
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

    /** Class name of a custom hover payload, used to recognise foreign mod actions. */
    public static String hoverPayloadClassName(HoverEvent hover) {
        Object value = hoverValue(hover);
        return value != null ? value.getClass().getName() : "";
    }
}
