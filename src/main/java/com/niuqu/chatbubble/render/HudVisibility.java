package com.niuqu.chatbubble.render;

// HUD 隐藏计数
public final class HudVisibility {
    private static int hides;

    // 私有构造
    private HudVisibility() {}

    // 增加隐藏层级
    public static void push() {
        hides++;
    }

    // 减少隐藏层级
    public static void pop() {
        if (hides > 0) hides--;
    }

    // 是否隐藏 HUD
    public static boolean shouldHideHud() {
        return hides > 0;
    }

    // 重置隐藏计数
    public static void reset() {
        hides = 0;
    }
}
