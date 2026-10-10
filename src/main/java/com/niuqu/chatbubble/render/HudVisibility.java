package com.niuqu.chatbubble.render;

public final class HudVisibility {
    private static int hides;

    private HudVisibility() {}

    public static void push() {
        hides++;
    }

    public static void pop() {
        if (hides > 0) hides--;
    }

    public static boolean shouldHideHud() {
        return hides > 0;
    }

    public static void reset() {
        hides = 0;
    }
}
