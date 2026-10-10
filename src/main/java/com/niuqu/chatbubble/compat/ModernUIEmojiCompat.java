package com.niuqu.chatbubble.compat;

import com.niuqu.chatbubble.E33Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.widget.TextFieldWidget;

public final class ModernUIEmojiCompat {
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(":[A-Za-z0-9_+\\-]+:");

    private static boolean resolved;
    private static boolean available;
    private static boolean enabledFlag = true;
    private static Object manager;
    private static Method lookupMethod;

    private ModernUIEmojiCompat() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> clientClass = Class.forName("icyllis.modernui.mc.ModernUIClient");
            try {
                Field enabledField = clientClass.getField("sEmojiShortcodes");
                enabledFlag = enabledField.getBoolean(null);
            } catch (NoSuchFieldException e) {
                enabledFlag = true;
            }
            Class<?> managerClass = Class.forName("icyllis.modernui.mc.FontResourceManager");
            Method getInstance = managerClass.getMethod("getInstance");
            manager = getInstance.invoke(null);
            lookupMethod = managerClass.getMethod("lookupEmojiShortcode", String.class);
            available = true;
        } catch (Throwable t) {
            E33Log.debug("[e33chat] ModernUI emoji shortcodes not available: {}", t.toString());
        }
    }

    public static boolean isEnabled() {
        resolve();
        return available && enabledFlag;
    }

    public static String lookup(String shortcode) {
        resolve();
        if (!available || shortcode == null) return null;
        try {
            return (String) lookupMethod.invoke(manager, shortcode);
        } catch (Throwable t) {
            E33Log.debug("[e33chat] ModernUI emoji lookup failed: {}", t.toString());
            return null;
        }
    }

    public static boolean replaceIn(TextFieldWidget field) {
        if (!isEnabled() || field == null) return false;
        String text = field.getText();
        if (text.indexOf(':') < 0 || text.startsWith("/")) return false;
        boolean any = false;
        while (true) {
            Matcher matcher = SHORTCODE_PATTERN.matcher(field.getText());
            boolean replaced = false;
            while (matcher.find()) {
                String shortcode = matcher.group();
                String replacement = lookup(shortcode);
                if (replacement != null) {
                    field.setSelectionStart(matcher.start());
                    field.setSelectionEnd(matcher.end());
                    field.write(replacement);
                    any = true;
                    replaced = true;
                    break;
                }
            }
            if (!replaced) break;
        }
        return any;
    }

    static String replaceAll(String text, Function<String, String> lookup) {
        Matcher matcher = SHORTCODE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length());
        int last = 0;
        while (matcher.find()) {
            String shortcode = matcher.group();
            String replacement = lookup.apply(shortcode);
            if (replacement == null) continue;
            sb.append(text, last, matcher.start()).append(replacement);
            last = matcher.end();
        }
        sb.append(text, last, text.length());
        return sb.toString();
    }
}
