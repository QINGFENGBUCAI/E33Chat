package com.niuqu.chatbubble.render;

// 滚动条尺寸计算
public final class ChatScrollbar {
    public static final int WIDTH = 6;
    private static final int MIN_THUMB_H = 8;

    // 私有构造
    private ChatScrollbar() {}

    // 计算滑块高度
    public static int thumbHeight(int trackH, int totalH) {
        if (totalH <= 0) return trackH;
        int h = Math.max(MIN_THUMB_H, (int) ((long) trackH * trackH / totalH));
        return Math.min(h, trackH);
    }

    // 计算滑块位置
    public static int thumbY(int trackTop, int trackH, int thumbH, int scrollOffset, int maxScroll) {
        int travelRange = trackH - thumbH;
        if (travelRange <= 0) return trackTop;
        return trackTop + (int) ((long) scrollOffset * travelRange / maxScroll);
    }
}
