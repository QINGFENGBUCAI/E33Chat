package com.niuqu.chatbubble.mixin;

//#if MC >= 11900
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// 屏幕控件操作访问器
@Mixin(Screen.class)
public interface ScreenAccessor {
    // 添加可绘制控件
    //#if MC >= 26000
    //$$ @Invoker("addRenderableWidget")
    //#else
    @Invoker("addDrawableChild")
    //#endif
    <T extends Element & Drawable & Selectable> T e33chat$addDrawableChild(T drawableElement);

    // 清空全部控件
    //#if MC >= 26000
    //$$ @Invoker("clearWidgets")
    //#else
    @Invoker("clearChildren")
    //#endif
    void e33chat$clearChildren();
}
//#else
//$$ public interface ScreenAccessor {
//$$ }
//#endif
