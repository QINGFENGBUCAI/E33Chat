package com.niuqu.chatbubble.mixin;

//#if MC >= 26000
// 26.x: the held-button state lives on MouseHandler's pressed booleans
// (GLFW is gone); "clear" means the left button is no longer held.
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
