package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.config.ChatBubbleConfig;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import com.niuqu.chatbubble.config.ChatBubbleConfig;

// 外观参数快照
public final class Appearance {
    // 私有构造
    private Appearance() {}

    // 取主题配色快照
    public static ChatBubbleTheme.Colors snapshot() {
        ChatBubbleConfig cfg = ChatBubbleClientSetup.config();
        ChatBubbleTheme theme = "light".equalsIgnoreCase(cfg.theme()) ? ChatBubbleTheme.LIGHT : ChatBubbleTheme.DARK;
        return theme.colors();
    }

    // 消息间距
    public static int messageGap() {
        Integer g = ChatBubbleClientSetup.config().messageGap();
        return g == null ? 6 : Math.max(0, Math.min(12, g));
    }

    // 头像尺寸
    public static int avatarSize() {
        Integer a = ChatBubbleClientSetup.config().avatarSize();
        return a == null ? 20 : Math.max(12, Math.min(32, a));
    }

    // 气泡尺寸像素
    public static int bubbleSizePx() {
        Integer s = ChatBubbleClientSetup.config().bubbleSize();
        return s == null ? 9 : Math.max(5, Math.min(14, s));
    }

    // 气泡缩放比例
    public static float bubbleScale(int fontHeight) {
        return Math.max(5, Math.min(14, bubbleSizePx())) / (float) fontHeight;
    }

    // 按比例换行宽度
    public static int scaledWrapWidth(int bubbleMaxW, float scale) {
        return Math.max(16, (int) (bubbleMaxW / scale));
    }

    // 气泡换行宽度
    public static int bubbleWrapWidth(int bubbleMaxW, int fontHeight) {
        return scaledWrapWidth(bubbleMaxW, bubbleScale(fontHeight));
    }
}
