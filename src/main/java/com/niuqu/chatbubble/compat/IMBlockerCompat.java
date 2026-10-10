package com.niuqu.chatbubble.compat;

import com.niuqu.chatbubble.E33Log;
import java.lang.reflect.Method;

public final class IMBlockerCompat {
    private static boolean resolved = false;
    private static boolean available = false;
    private static Method setPreferredEnglishState;

    private IMBlockerCompat() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> iface = Class.forName("io.github.reserveword.imblocker.common.gui.MinecraftTextFieldWidget");
            setPreferredEnglishState = iface.getMethod("setPreferredEnglishState", boolean.class);
            available = true;
        } catch (Throwable t) {
            E33Log.debug("[e33chat] IMBlocker not present, IME state sync disabled: {}", t.toString());
        }
    }

    public static void setCommandMode(Object textField, boolean command) {
        resolve();
        if (!available || textField == null) return;
        try {
            setPreferredEnglishState.invoke(textField, command);
        } catch (Throwable t) {
            E33Log.debug("[e33chat] IMBlocker sync failed: {}", t.toString());
        }
    }
}
