package com.niuqu.chatbubble.render;
import java.util.List;

// 文本选区管理
public final class ChatTextSelection {
    private static final int SELECTION_BG = 0xE02D6FD6;
    private static final int SELECTION_FG = 0xFFFFFFFF;

    // 选区背景色
    public static int selectionBg() {
        return SELECTION_BG;
    }

    // 选区前景色
    public static int selectionFg() {
        return SELECTION_FG;
    }

    private int anchorMsg = -1, anchorLine = -1, anchorKind = -1, anchorChar = -1;
    private int focusMsg = -1, focusLine = -1, focusKind = -1, focusChar = -1;
    private boolean dragActive;
    private boolean moved;

    // 开始选区拖拽
    public void begin(int msg, int line, int kind, int ch) {
        anchorMsg = focusMsg = msg;
        anchorLine = focusLine = line;
        anchorKind = focusKind = kind;
        anchorChar = focusChar = ch;
        dragActive = true;
        moved = false;
    }

    // 更新选区焦点
    public void update(int msg, int line, int kind, int ch) {
        if (!dragActive) return;
        focusMsg = msg;
        focusLine = line;
        focusKind = kind;
        focusChar = ch;
    }

    // 结束拖拽
    public void endDrag() {
        dragActive = false;
    }

    // 是否拖拽中
    public boolean isDragActive() {
        return dragActive;
    }

    // 是否发生移动
    public boolean didMove() {
        return moved;
    }

    // 标记已移动
    public void markMoved() {
        if (dragActive) moved = true;
    }

    // 清空选区
    public void clear() {
        anchorMsg = focusMsg = -1;
        anchorLine = focusLine = -1;
        anchorKind = focusKind = -1;
        anchorChar = focusChar = -1;
        dragActive = false;
        moved = false;
    }

    // 是否存在选区
    public boolean hasSelection() {
        if (anchorMsg < 0 || focusMsg < 0) return false;
        if (anchorMsg != focusMsg || anchorLine != focusLine || anchorKind != focusKind) {
            return true;
        }
        return anchorChar != focusChar;
    }

    // 计算跨行定位键
    private static long key(int msg, int line, int kind) {
        return (long) msg * 100_000L + kind * 10_000L + line;
    }

    // 计算选区行范围
    public int[] rangeFor(TextSpan span) {
        if (anchorMsg < 0 || focusMsg < 0) return null;
        long a = key(anchorMsg, anchorLine, anchorKind);
        long f = key(focusMsg, focusLine, focusKind);
        long s = span.orderKey();
        int len = span.text().codePointCount(0, span.text().length());
        int start;
        int end;
        if (a == f) {
            if (s != a) return null;
            start = Math.min(anchorChar, focusChar);
            end = Math.max(anchorChar, focusChar);
        } else if (a < f) {
            if (s < a || s > f) return null;
            start = (s == a) ? anchorChar : 0;
            end = (s == f) ? focusChar : len;
        } else {
            if (s < f || s > a) return null;
            start = (s == f) ? focusChar : 0;
            end = (s == a) ? anchorChar : len;
        }
        start = Math.max(0, Math.min(len, start));
        end = Math.max(0, Math.min(len, end));
        if (end <= start) return null;
        return new int[]{start, end};
    }

    // 复制选中文本
    public String copyText(List<TextSpan> spans) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (TextSpan span : spans) {
            int[] r = rangeFor(span);
            if (r == null) continue;
            if (!first) sb.append('\n');
            int cs = span.text().offsetByCodePoints(0, r[0]);
            int ce = span.text().offsetByCodePoints(0, r[1]);
            sb.append(span.text(), cs, ce);
            first = false;
        }
        return sb.toString();
    }
}
