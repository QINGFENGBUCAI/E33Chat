package com.niuqu.chatbubble.compat;

/**
 * 1.21.9 input-rework helpers.
 *
 * <p>1.21.9 replaced the flat {@code (keyCode, scanCode, modifiers)} /
 * {@code (mouseX, mouseY, button)} parameter lists on {@code Element} with the
 * {@code KeyInput} / {@code Click} records. Call sites in the 1.21.9-1.21.11 range
 * are rewritten by build.gradle to construct those records here; older versions
 * keep the flat form and never reference this class.</p>
 *
 * <p>26.x 沿用同一套 record 化输入 API（{@code Click} → {@code MouseButtonEvent}、
 * {@code KeyInput} → {@code KeyEvent}），但 {@code Screen.hasShiftDown()} 依然不存在，
 * 且 {@code KeyMapping.matches} 只接受 record 参数，因此 26.x 分支单独提供实现。
 * 这里的类名/包名照旧写成 Yarn 形式，由 build.gradle 的 applyMojangMapping 统一重写。</p>
 */
public final class InputCompat {
    private InputCompat() {}

    //#if MC >= 12109
    //#if MC >= 26000
    public static net.minecraft.client.gui.Click click(double x, double y, int button) {
        return new net.minecraft.client.gui.Click(x, y,
            new net.minecraft.client.input.MouseInput(button, 0));
    }

    public static net.minecraft.client.input.KeyInput key(int keyCode, int scanCode, int modifiers) {
        return new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers);
    }

    /** {@code widget.mouseClicked(x, y, button)} → {@code mouseClicked(MouseButtonEvent, boolean)}. */
    public static boolean mouseClicked(net.minecraft.client.gui.widget.ClickableWidget widget,
                                       double x, double y, int button) {
        return widget.mouseClicked(click(x, y, button), false);
    }

    /** {@code widget.onClick(x, y)} → {@code onClick(MouseButtonEvent, boolean)}. */
    public static void onClick(net.minecraft.client.gui.widget.ClickableWidget widget,
                               double x, double y) {
        widget.onClick(click(x, y, 0), false);
    }

    /** {@code keyChat.matchesKey(keyCode, scanCode)} → {@code keyChat.matches(KeyEvent)}。 */
    public static boolean keyMatches(net.minecraft.client.KeyBinding binding, int keyCode, int scanCode) {
        return binding.matches(key(keyCode, scanCode, 0));
    }

    /** 26.x 仍然没有 {@code Screen.hasShiftDown()}，改用左 Shift 绑定当前的按下状态。 */
    public static boolean hasShiftDown() {
        return net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown();
    }
    //#else
    public static net.minecraft.client.gui.Click click(double x, double y, int button) {
        return new net.minecraft.client.gui.Click(x, y,
            new net.minecraft.client.input.MouseInput(button, 0));
    }

    public static net.minecraft.client.input.KeyInput key(int keyCode, int scanCode, int modifiers) {
        return new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers);
    }

    /** {@code widget.mouseClicked(x, y, button)} → {@code mouseClicked(Click, boolean)}. */
    public static boolean mouseClicked(net.minecraft.client.gui.widget.ClickableWidget widget,
                                       double x, double y, int button) {
        return widget.mouseClicked(click(x, y, button), false);
    }

    /** {@code widget.onClick(x, y)} → {@code onClick(Click, boolean)}. */
    public static void onClick(net.minecraft.client.gui.widget.ClickableWidget widget,
                               double x, double y) {
        widget.onClick(click(x, y, 0), false);
    }

    /** {@code Screen.hasShiftDown()} was removed in 1.21.9 along with the flat input params. */
    public static boolean hasShiftDown() {
        return net.minecraft.client.util.InputUtil.isKeyPressed(
            net.minecraft.client.MinecraftClient.getInstance().getWindow(),
            net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT_SHIFT);
    }
    //#endif
    //#endif
}
