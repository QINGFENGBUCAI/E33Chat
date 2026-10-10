package com.niuqu.chatbubble.mixin;

//#if MC >= 26000

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("isLeftPressed")
    void e33chat$setActiveButton(boolean held);
}
//#else
//$$ import net.minecraft.client.Mouse;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ /** Lets the AWT file dialog clear MC's held-button state while it owns input. */
//$$ @Mixin(Mouse.class)
//$$ public interface MouseHandlerAccessor {
//$$     @Accessor("activeButton")
//$$     void e33chat$setActiveButton(int button);
//$$ }
//#endif
