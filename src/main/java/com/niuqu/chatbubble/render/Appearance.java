package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.ChatBubbleScreen;
import com.niuqu.chatbubble.config.ChatBubbleConfig;
import com.niuqu.chatbubble.render.ChatBubbleTheme;

import com.niuqu.chatbubble.config.ChatBubbleConfig;

public final class Appearance {
    private Appearance() {}

    public static ChatBubbleTheme.Colors snapshot() {
        ChatBubbleConfig cfg = ChatBubbleClientSetup.config();
        ChatBubbleTheme theme = "light".equalsIgnoreCase(cfg.theme()) ? ChatBubbleTheme.LIGHT : ChatBubbleTheme.DARK;
        return theme.colors();
    }

    public static int messageGap() {
        Integer g = ChatBubbleClientSetup.config().messageGap();
        return g == null ? 6 : Math.max(0, Math.min(12, g));
    }

    public static int avatarSize() {
        Integer a = ChatBubbleClientSetup.config().avatarSize();
        return a == null ? 20 : Math.max(12, Math.min(32, a));
    }

    public static int bubbleSizePx() {
        Integer s = ChatBubbleClientSetup.config().bubbleSize();
        return s == null ? 9 : Math.max(5, Math.min(14, s));
    }

    public static float bubbleScale(int fontHeight) {
        return Math.max(5, Math.min(14, bubbleSizePx())) / (float) fontHeight;
    }

    public static int scaledWrapWidth(int bubbleMaxW, float scale) {
        return Math.max(16, (int) (bubbleMaxW / scale));
    }

    public static int bubbleWrapWidth(int bubbleMaxW, int fontHeight) {
        return scaledWrapWidth(bubbleMaxW, bubbleScale(fontHeight));
    }
}
