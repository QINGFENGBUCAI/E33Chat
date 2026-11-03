//#if MC < 12000
package com.niuqu.chatbubble;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
//#if MC < 11903
import net.minecraft.util.math.Matrix4f;
//#endif
import com.mojang.blaze3d.systems.RenderSystem;
//#if MC >= 11700
//#endif
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

// 旧版绘制上下文封装
public class DrawContext extends DrawableHelper {
    private final MatrixStack matrices;
    private final VertexConsumerProvider.Immediate immediate;

    // 构造绘制上下文
    public DrawContext(MatrixStack matrices) {
        this.matrices = matrices;
        this.immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
    }

    // 获取矩阵栈
    public MatrixStack getMatrices() {
        return matrices;
    }

    // 提交顶点缓存
    public void draw() {
        immediate.draw();
    }

    // 填充矩形
    public void fill(int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(matrices, x1, y1, x2, y2, color);
    }

    // 填充渐变矩形
    public void fillGradient(int x1, int y1, int x2, int y2, int color1, int color2) {
        fillGradient(matrices, x1, y1, x2, y2, color1, color2);
    }

    // 绘制文本
    public int drawText(TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        return drawText(renderer, text.asOrderedText(), x, y, color, shadow);
    }

    // 绘制字符串
    public int drawText(TextRenderer renderer, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            return renderer.drawWithShadow(matrices, text, x, y, color);
        } else {
            return renderer.draw(matrices, text, x, y, color);
        }
    }

    // 绘制有序文本
    public int drawText(TextRenderer renderer, OrderedText text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            return renderer.drawWithShadow(matrices, text, x, y, color);
        } else {
            return renderer.draw(matrices, text, x, y, color);
        }
    }

    // 绘制纹理
    public void drawTexture(Identifier texture, int x, int y, float u, float v, int width, int height) {
        drawTexture(texture, x, y, width, height, u, v, width, height, 256, 256);
    }

    // 绘制区域纹理
    public void drawTexture(Identifier texture, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight) {
        drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, 256, 256);
    }

    // 绘制裁剪纹理
    public void drawTexture(Identifier texture, int x, int y, int width, int height,
                            float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        //#if MC >= 11800
        RenderSystem.setShaderTexture(0, texture);
        DrawableHelper.drawTexture(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        //#else
        //$$ MinecraftClient.getInstance().getTextureManager().bindTexture(texture);
        //$$ Matrix4f model = matrices.peek().getModel();
        //$$ drawTexturedQuad(model, x, x + width, y, y + height,
        //$$         u / textureWidth, (u + regionWidth) / textureWidth,
        //$$         v / textureHeight, (v + regionHeight) / textureHeight);
        //#endif
    }

    // 绘制整数坐标纹理
    public void drawTexture(Identifier texture, int x, int y, int width, int height,
                            int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        drawTexture(texture, x, y, width, height, (float) u, (float) v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    //#if MC < 11800
    //$$ private static void drawTexturedQuad(Matrix4f matrix, int x1, int x2, int y1, int y2,
    //$$         float u1, float u2, float v1, float v2) {
    //$$     BufferBuilder buffer = Tessellator.getInstance().getBuffer();
    //$$     buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
    //$$     buffer.vertex(matrix, x1, y2, 0).texture(u1, v2).next();
    //$$     buffer.vertex(matrix, x2, y2, 0).texture(u2, v2).next();
    //$$     buffer.vertex(matrix, x2, y1, 0).texture(u2, v1).next();
    //$$     buffer.vertex(matrix, x1, y1, 0).texture(u1, v1).next();
    //$$     buffer.end();
    //$$     BufferRenderer.draw(buffer);
    //$$ }
    //#endif
    //#if MC < 11900
    // 启用裁剪区域
    public void enableScissor(int x1, int y1, int x2, int y2) {
        RenderSystem.enableScissor(x1, y1, x2, y2);
    }

    // 关闭裁剪区域
    public void disableScissor() {
        RenderSystem.disableScissor();
    }
    //#endif

    // 绘制提示文本
    public void drawTooltip(TextRenderer renderer, Text text, int x, int y) {
    }

    // 绘制多行提示
    public void drawTooltip(TextRenderer renderer, java.util.List<OrderedText> lines, Object positioner, int x, int y, boolean useShadow) {
    }

    // 绘制多行提示
    public void drawTooltip(TextRenderer renderer, java.util.List<OrderedText> lines, Object positioner, int x, int y) {
    }

    // 绘制矩形边框
    public void drawBorder(int x, int y, int w, int h, int color) {
        fill(x, y, x + w, y + 1, color);
        fill(x, y + h - 1, x + w, y + h, color);
        fill(x, y, x + 1, y + h, color);
        fill(x + w - 1, y, x + w, y + h, color);
    }

    // 处理悬停事件
    public void drawHoverEvent(TextRenderer renderer, net.minecraft.text.Style style, int x, int y) {
    }

    // 设置帧提示
    public void setTooltipForNextFrame(net.minecraft.text.Text text, int x, int y) {
    }
}
//#endif
