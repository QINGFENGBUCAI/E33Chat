package com.niuqu.chatbubble.render;

//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif

public final class RoundRectRenderer {
    private RoundRectRenderer() {}

    private static final float BAND = 0.75f;

    private static final int N = 4;

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

    private static void solid(DrawContext g, int x1, int y1, int x2, int y2, int argb) {
        //#if MC >= 12000
        com.niuqu.chatbubble.RenderHelper.fill(g, x1, y1, x2, y2, argb);
        //#else
        //$$ g.fill(x1, y1, x2, y2, argb);
        //#endif
    }
}
