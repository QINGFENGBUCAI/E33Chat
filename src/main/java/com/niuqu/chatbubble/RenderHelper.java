package com.niuqu.chatbubble;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import net.minecraft.client.gui.DrawableHelper;
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

// 渲染接口版本适配
public final class RenderHelper {
    // 禁止外部实例化
    private RenderHelper() {}

    // 填充矩形
    public static void fill(Object ctx, int x1, int y1, int x2, int y2, int color) {
        //#if MC >= 12000
        ((DrawContext) ctx).fill(x1, y1, x2, y2, color);
        //#else
        //$$ DrawableHelper.fill((MatrixStack) ctx, x1, y1, x2, y2, color);
        //#endif
    }
}
