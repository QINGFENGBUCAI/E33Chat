package com.niuqu.chatbubble.texture;

//#if MC >= 12105
import com.niuqu.chatbubble.DrawHelper;
//#endif
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
//#if MC < 12105
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
//#endif
import net.minecraft.util.Identifier;

/**
 * 带整体透明度的纹理渲染：纹理色 × 白 (1,1,1,alpha)。
 * 用于动态 alpha 的元素（面板开屏淡入、滚动条淡入淡出）——普通 drawTexture 无法携带运行时透明度。
 *
 * 1.21.5 移除了 RenderSystem 的 shader/blend 入口（渲染状态并入 RenderPipeline），
 * 改用 DrawContext.drawTexture 的颜色参数承载 tint/alpha——语义等价且无需自建 quad。
 */
public final class ColoredTextureRenderer {

    private ColoredTextureRenderer() {}

    public static void drawWithAlpha(DrawContext g, Identifier tex,
                                     int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0.003f) return;
        //#if MC >= 12105
        // 采样整张纹理：regionW/texW = 1/1 使 UV 落在 0..1，无需知道纹理实际尺寸
        DrawHelper.drawTexture(g, tex, x, y, w, h, 0f, 0f, 1, 1, 1, 1, pack(1f, 1f, 1f, alpha));
        //#else
        //$$ g.draw();
        //$$ RenderSystem.setShaderTexture(0, tex);
        //$$ RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        //$$ boolean blendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        //$$ RenderSystem.enableBlend();
        //$$ RenderSystem.defaultBlendFunc();
        //$$ Matrix4f pose = g.getMatrices().peek().getPositionMatrix();
        //$$ BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //$$ bb.vertex(pose, x, y, 0).texture(0f, 0f).color(1f, 1f, 1f, alpha);
        //$$ bb.vertex(pose, x, y + h, 0).texture(0f, 1f).color(1f, 1f, 1f, alpha);
        //$$ bb.vertex(pose, x + w, y + h, 0).texture(1f, 1f).color(1f, 1f, 1f, alpha);
        //$$ bb.vertex(pose, x + w, y, 0).texture(1f, 0f).color(1f, 1f, 1f, alpha);
        //$$ BufferRenderer.drawWithGlobalProgram(bb.end());
        //$$ if (!blendEnabled) RenderSystem.disableBlend();
        //#endif
    }

    /** 带整体 tint 色的纹理渲染：纹理色 × tint(r,g,b,a)。用于白色默认纹理 × 主题色动态着色。 */
    public static void drawTinted(DrawContext g, Identifier tex,
                                  int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        //#if MC >= 12105
        DrawHelper.drawTexture(g, tex, x, y, w, h, 0f, 0f, 1, 1, 1, 1, argb);
        //#else
        //$$ float a = (argb >>> 24) / 255f;
        //$$ float r = (argb >> 16 & 0xFF) / 255f;
        //$$ float gr = (argb >> 8 & 0xFF) / 255f;
        //$$ float b = (argb & 0xFF) / 255f;
        //$$ g.draw();
        //$$ RenderSystem.setShaderTexture(0, tex);
        //$$ RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        //$$ boolean blendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        //$$ RenderSystem.enableBlend();
        //$$ RenderSystem.defaultBlendFunc();
        //$$ Matrix4f pose = g.getMatrices().peek().getPositionMatrix();
        //$$ BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //$$ bb.vertex(pose, x, y, 0).texture(0f, 0f).color(r, gr, b, a);
        //$$ bb.vertex(pose, x, y + h, 0).texture(0f, 1f).color(r, gr, b, a);
        //$$ bb.vertex(pose, x + w, y + h, 0).texture(1f, 1f).color(r, gr, b, a);
        //$$ bb.vertex(pose, x + w, y, 0).texture(1f, 0f).color(r, gr, b, a);
        //$$ BufferRenderer.drawWithGlobalProgram(bb.end());
        //$$ if (!blendEnabled) RenderSystem.disableBlend();
        //#endif
    }

    /**
     * 带整体透明度 + UV 采样的纹理渲染：等价 drawTexture 的
     * (u,v,regionWidth,regionHeight,textureWidth,textureHeight) 语义，但带动态 alpha。
     * 图标/带采样区域的元素淡入用（drawTexture 走 POSITION_TEXTURE 不吃 setShaderColor）。
     */
    public static void drawWithAlpha(DrawContext g, Identifier tex,
                                     int x, int y, int w, int h,
                                     float u, float v, int regionW, int regionH,
                                     int texW, int texH, float alpha) {
        drawWithAlphaTinted(g, tex, x, y, w, h, u, v, regionW, regionH, texW, texH, alpha, 1f, 1f, 1f);
    }

    /**
     * 带灰度 + 透明度 + UV 采样的纹理渲染：用于离线玩家头像灰显。
     * grayLevel < 1 时将 RGB 通道乘以 grayLevel 实现灰度效果。
     */
    public static void drawWithAlphaGrayscale(DrawContext g, Identifier tex,
                                              int x, int y, int w, int h,
                                              float u, float v, int regionW, int regionH,
                                              int texW, int texH, float alpha, float grayLevel) {
        drawWithAlphaTinted(g, tex, x, y, w, h, u, v, regionW, regionH, texW, texH, alpha, grayLevel, grayLevel, grayLevel);
    }

    private static void drawWithAlphaTinted(DrawContext g, Identifier tex,
                                            int x, int y, int w, int h,
                                            float u, float v, int regionW, int regionH,
                                            int texW, int texH, float alpha,
                                            float r, float gr, float b) {
        if (w <= 0 || h <= 0 || alpha <= 0.003f) return;
        //#if MC >= 12105
        DrawHelper.drawTexture(g, tex, x, y, w, h, u, v, regionW, regionH, texW, texH,
            pack(r, gr, b, alpha));
        //#else
        //$$ g.draw();
        //$$ RenderSystem.setShaderTexture(0, tex);
        //$$ RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        //$$ boolean blendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        //$$ RenderSystem.enableBlend();
        //$$ RenderSystem.defaultBlendFunc();
        //$$ float u1 = u / texW, u2 = (u + regionW) / texW;
        //$$ float v1 = v / texH, v2 = (v + regionH) / texH;
        //$$ Matrix4f pose = g.getMatrices().peek().getPositionMatrix();
        //$$ BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //$$ bb.vertex(pose, x, y, 0).texture(u1, v1).color(r, gr, b, alpha);
        //$$ bb.vertex(pose, x, y + h, 0).texture(u1, v2).color(r, gr, b, alpha);
        //$$ bb.vertex(pose, x + w, y + h, 0).texture(u2, v2).color(r, gr, b, alpha);
        //$$ bb.vertex(pose, x + w, y, 0).texture(u2, v1).color(r, gr, b, alpha);
        //$$ BufferRenderer.drawWithGlobalProgram(bb.end());
        //$$ if (!blendEnabled) RenderSystem.disableBlend();
        //#endif
    }

    //#if MC >= 12105
    private static int pack(float r, float g, float b, float a) {
        int ai = Math.round(Math.max(0f, Math.min(1f, a)) * 255f);
        int ri = Math.round(Math.max(0f, Math.min(1f, r)) * 255f);
        int gi = Math.round(Math.max(0f, Math.min(1f, g)) * 255f);
        int bi = Math.round(Math.max(0f, Math.min(1f, b)) * 255f);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
    //#endif
}
