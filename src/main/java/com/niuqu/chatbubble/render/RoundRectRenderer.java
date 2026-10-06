package com.niuqu.chatbubble.render;

//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif

/**
 * 圆角矩形绘制（全版本统一实现）。
 *
 * <p>用逐行扫描线 + SDF 覆盖率做抗锯齿：整行满覆盖的像素合并成一段
 * {@code fill}，只有圆角过渡带内的像素才逐个按比例 alpha 填充。只依赖
 * {@code DrawContext.fill} / {@link com.niuqu.chatbubble.RenderHelper#fill}，
 * 不使用自定义 shader 或 RenderSystem 状态，因此在全部 21 个目标版本上行为一致。</p>
 *
 * <p>历史：早期版本在 1.19.3~1.21.1 上走 {@code rendertype_round_rect} 自定义
 * shader，1.21.2+ 直接退化成直角填充。shader 路径会泄漏 blend/shader 状态
 * （上游同样因此移除，退出服务器时黑屏），且 1.21.2~26.2 共 12 个版本完全没有圆角。
 * 现在统一为扫描线实现，直角退化与 shader 依赖一并删除。</p>
 */
public final class RoundRectRenderer {

    private RoundRectRenderer() {}

    /**
     * SDF 过渡带半宽（GUI 像素）。带越宽，alpha 渐变跨越越多像素，
     * 可以盖住高 GUI 缩放下 GUI 像素的阶梯感。
     */
    private static final float BAND = 0.75f;

    /** 每轴超采样数：每个 GUI 像素切成 N×N 个子样本取平均。 */
    private static final int N = 4;

    /**
     * 保留的空实现：历史上用于资源重载后重置自定义 shader。
     * 当前实现不加载 shader，调用点无需改动。
     */
    public static void resetShader() {
    }

    public static void fill(DrawContext g, int x1, int y1, int x2, int y2, float radius, int argb) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w, h) / 2f);
        if (radius < 1f) {
            solid(g, x1, y1, x2, y2, argb);
            return;
        }

        int cr = (int) Math.ceil(radius + BAND);
        int baseAlpha = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        int fullColor = (baseAlpha << 24) | rgb;

        // 逐行扫描线：满覆盖区间合并为一段 fill，圆角处逐像素按覆盖率混合。
        // 相比「3 个实心矩形 + 4 个角格」的写法，行内不会出现水平接缝
        // （延迟渲染管线上会显示成细线）。
        for (int py = 0; py < h; py++) {
            int rowY = y1 + py;
            if (py >= cr && py < h - cr) {
                solid(g, x1, rowY, x2, rowY + 1, fullColor);
                continue;
            }
            int runStart = -1;
            for (int px = 0; px < w; px++) {
                float cov = pixelCoverage(px, py, w, h, radius, cr);
                if (cov >= 1f) {
                    if (runStart < 0) runStart = px;
                } else {
                    if (runStart >= 0) {
                        solid(g, x1 + runStart, rowY, x1 + px, rowY + 1, fullColor);
                        runStart = -1;
                    }
                    if (cov > 0f) {
                        int a = (int) (baseAlpha * cov);
                        solid(g, x1 + px, rowY, x1 + px + 1, rowY + 1, (a << 24) | rgb);
                    }
                }
            }
            if (runStart >= 0) solid(g, x1 + runStart, rowY, x1 + w, rowY + 1, fullColor);
        }
    }

    /**
     * 单个 GUI 像素的圆角 SDF 覆盖率，N×N 子样本取平均。不在四个角格内
     * （即直边区域）的像素直接返回 1。圆心按像素所在半平面选取，窄矩形的
     * 角区重叠时也能取到正确的最近圆弧。
     */
    private static float pixelCoverage(int px, int py, int w, int h, float radius, int cr) {
        boolean nearLeft = px < cr;
        boolean nearRight = px >= w - cr;
        boolean nearTop = py < cr;
        boolean nearBottom = py >= h - cr;
        if (!((nearLeft || nearRight) && (nearTop || nearBottom))) return 1f;
        float cx = (px < w / 2f) ? radius : w - radius;
        float cy = (py < h / 2f) ? radius : h - radius;
        float covSum = 0f;
        for (int sy = 0; sy < N; sy++) {
            float pyy = py + (sy + 0.5f) / N;
            float dy = pyy - cy;
            float dy2 = dy * dy;
            for (int sx = 0; sx < N; sx++) {
                float pxx = px + (sx + 0.5f) / N;
                float dx = pxx - cx;
                float d = (float) Math.sqrt(dx * dx + dy2);
                float cov = (radius + BAND - d) / (2 * BAND);
                covSum += cov < 0f ? 0f : (cov > 1f ? 1f : cov);
            }
        }
        return covSum / (N * N);
    }

    /**
     * 一段实心横条。1.20+ 走 {@link com.niuqu.chatbubble.RenderHelper#fill}
     * （与其它 UI 元素同一条绘制路径）；1.16.5~1.19.2 走本地 DrawContext polyfill。
     */
    private static void solid(DrawContext g, int x1, int y1, int x2, int y2, int argb) {
        //#if MC >= 12000
        com.niuqu.chatbubble.RenderHelper.fill(g, x1, y1, x2, y2, argb);
        //#else
        //$$ g.fill(x1, y1, x2, y2, argb);
        //#endif
    }
}
