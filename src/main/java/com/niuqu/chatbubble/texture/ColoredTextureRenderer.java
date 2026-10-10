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

public final class ColoredTextureRenderer {

    private ColoredTextureRenderer() {}

    public static void drawWithAlpha(DrawContext g, Identifier tex,
                                     int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0.003f) return;
        //#if MC >= 12105

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

    public static void drawWithAlpha(DrawContext g, Identifier tex,
                                     int x, int y, int w, int h,
                                     float u, float v, int regionW, int regionH,
                                     int texW, int texH, float alpha) {
        drawWithAlphaTinted(g, tex, x, y, w, h, u, v, regionW, regionH, texW, texH, alpha, 1f, 1f, 1f);
    }

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
