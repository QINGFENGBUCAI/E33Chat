package com.niuqu.chatbubble;

//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12000
public class DrawHelper {
    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        //#if MC >= 12106
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //#else
        //#if MC >= 12102
        //$$ context.drawTexture(id -> net.minecraft.client.render.RenderLayer.getGuiTextured(id), texture, x, y, (int)u, (int)v, width, height, textureWidth, textureHeight);
        //#else
        //$$ context.drawTexture(texture, x, y, (int)u, (int)v, width, height, textureWidth, textureHeight);
        //#endif
        //#endif
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        //#if MC >= 12106
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight, color);
        //#else
        //#if MC >= 12102
        //$$ context.drawTexture(id -> net.minecraft.client.render.RenderLayer.getGuiTextured(id), texture, x, y, (int)u, (int)v, width, height, textureWidth, textureHeight, color);
        //#else
        //$$ context.drawTexture(texture, x, y, (int)u, (int)v, width, height, textureWidth, textureHeight);
        //$$ // 1.21.1 不支持颜色参数，忽略
        //#endif
        //#endif
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        //#if MC >= 12106
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
        //#else
        //#if MC >= 12102
        //$$ context.drawTexture(id -> net.minecraft.client.render.RenderLayer.getGuiTextured(id), texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
        //#else
        //$$ context.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        //#endif
        //#endif
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        //#if MC >= 12106
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight, color);
        //#else
        //#if MC >= 12102
        //$$ context.drawTexture(id -> net.minecraft.client.render.RenderLayer.getGuiTextured(id), texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight, color);
        //#else
        //$$ context.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        //$$ // 1.21.1 不支持颜色参数，忽略
        //#endif
        //#endif
    }
}
//#else
//$$ public class DrawHelper {
//$$ }
//#endif
