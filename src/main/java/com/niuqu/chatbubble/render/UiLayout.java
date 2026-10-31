package com.niuqu.chatbubble.render;

// 界面布局计算
public final class UiLayout {
    // 私有构造
    private UiLayout() {}

    // 计算水平居中 X
    public static int centerX(int containerLeft, int containerWidth, int elemWidth) {
        return containerLeft + (containerWidth - elemWidth) / 2;
    }

    // 限制 X 最小值
    public static int clampX(int x, int minX) {
        return Math.max(x, minX);
    }
}
