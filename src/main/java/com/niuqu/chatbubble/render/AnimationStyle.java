package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.render.Animation;

public enum AnimationStyle {
    SLIDE,
    FADE,
    ZOOM,
    NONE;

    public static AnimationStyle parse(String s) {
        if (s == null) return SLIDE;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SLIDE;
        }
    }
}
