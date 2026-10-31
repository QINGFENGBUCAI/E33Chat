package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.render.Animation;

// 动画样式枚举
public enum AnimationStyle {
    SLIDE,
    FADE,
    ZOOM,
    NONE;

    // 解析样式名称
    public static AnimationStyle parse(String s) {
        if (s == null) return SLIDE;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SLIDE;
        }
    }
}
