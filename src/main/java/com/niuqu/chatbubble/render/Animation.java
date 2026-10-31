package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.render.AnimationStyle;
import net.minecraft.util.math.MathHelper;

// 动画缓动工具
public final class Animation {
    // 私有构造
    private Animation() {}

    // 缓出三次曲线
    public static float easeOutCubic(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    // 缓出二次曲线
    public static float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    // 回弹缓出曲线
    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    // 按样式取曲线值
    public static float styleCurve(AnimationStyle style, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        if (style == null || style == AnimationStyle.NONE) return 1f;
        if (style == AnimationStyle.FADE) return easeOutQuad(t);
        return easeOutCubic(t);
    }

    // 平滑插值逼近
    public static float lerpTo(float current, float target, float speed, float snapThreshold) {
        float next = current + (target - current) * speed;
        if (Math.abs(next - target) < snapThreshold) return target;
        return next;
    }

    // 淡入透明度
    public static int fadeIn(int ticks, int duration) {
        if (duration <= 0 || ticks >= duration) return 255;
        return ticks * 255 / duration;
    }

    // 淡出透明度
    public static int fadeOut(int ticks, int duration) {
        if (duration <= 0 || ticks >= duration) return 0;
        return (duration - ticks) * 255 / duration;
    }

    // 淡入淡出透明度
    public static int fadeInOut(int ticks, int fadeInDur, int holdDur, int fadeOutDur) {
        if (ticks < fadeInDur) return fadeIn(ticks, fadeInDur);
        ticks -= fadeInDur;
        if (ticks < holdDur) return 255;
        ticks -= holdDur;
        return fadeOut(ticks, fadeOutDur);
    }

    // 计算动画进度
    public static float progress(long startMs, int durationMs, boolean closing) {
        long elapsed = net.minecraft.util.Util.getMeasuringTimeMs() - startMs;
        float t = MathHelper.clamp((float) elapsed / durationMs, 0f, 1f);
        if (closing) return 1.0f - (t * t);
        return easeOutCubic(t);
    }
}
