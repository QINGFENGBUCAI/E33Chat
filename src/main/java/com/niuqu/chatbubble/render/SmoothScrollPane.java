package com.niuqu.chatbubble.render;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

// 平滑滚动容器
public class SmoothScrollPane {
    private int offset;
    private float animFrom, animTo;
    private long animStart;
    private int animDur;
    private boolean animOn;
    private boolean barDrag;
    private int barDragY, barDragOff;

    // 构造滚动容器
    public SmoothScrollPane() {}

    // 读取当前偏移
    public int offset() { return offset; }

    // 设置当前偏移
    public void setOffset(int v) { offset = v; }

    // 平滑滚动到目标
    public void animateTo(float target, int max, int dur) {
        animFrom = offset;
        animTo = MathHelper.clamp(target, 0, max);
        animStart = Util.getMeasuringTimeMs();
        animDur = dur;
        animOn = true;
    }

    // 每帧推进动画
    public void tick(int max) {
        if (animOn) {
            float t = Animation.progress(animStart, animDur, false);
            offset = Math.round(animFrom + (animTo - animFrom) * t);
            if (t >= 1.0f) { offset = Math.round(animTo); animOn = false; }
        }
        offset = MathHelper.clamp(offset, 0, max);
    }

    // 是否拖拽中
    public boolean dragging() { return barDrag; }

    // 开始拖拽滚动条
    public void dragStart(int mouseY, int currentOffset) {
        barDrag = true;
        barDragY = mouseY;
        barDragOff = currentOffset;
    }

    // 拖拽调整偏移
    public void dragTo(int mouseY, int trackH, int totalH, int maxScroll, int dur) {
        if (!barDrag || maxScroll <= 0) return;
        int travel = trackH - ChatScrollbar.thumbHeight(trackH, totalH);
        if (travel > 0) {
            int d = mouseY - barDragY;
            animateTo(barDragOff + (float) d * maxScroll / travel, maxScroll, dur);
        }
    }

    // 结束拖拽滚动条
    public void dragEnd() { barDrag = false; }

    // 滚轮滚动
    public void wheel(double delta, int max, int dur) {
        animateTo(offset - (int) (delta * 20), max, dur);
    }
}
