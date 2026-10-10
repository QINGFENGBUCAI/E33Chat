package com.niuqu.chatbubble.compat;

public final class InputCompat {
    private InputCompat() {}

    //#if MC >= 12109
    //#if MC >= 26000
    /** 构造传给原版 widget 的鼠标事件：调用方传 GLFW 风格按钮值，这里逆换算成
     *  26.3 的 HID 编号（widget 的 isValidClickButton 按新编号判定）。 */
    public static net.minecraft.client.gui.Click click(double x, double y, int button) {
        return new net.minecraft.client.gui.Click(x, y,
            new net.minecraft.client.input.MouseInput(rawButton(button), 0));
    }

    public static net.minecraft.client.input.KeyInput key(int keyCode, int scanCode, int modifiers) {
        return new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers);
    }

    public static boolean mouseClicked(net.minecraft.client.gui.widget.ClickableWidget widget,
                                       double x, double y, int button) {
        return widget.mouseClicked(click(x, y, button), false);
    }

    public static void onClick(net.minecraft.client.gui.widget.ClickableWidget widget,
                               double x, double y) {
        widget.onClick(click(x, y, 0), false);
    }

    public static boolean keyMatches(net.minecraft.client.KeyBinding binding, int keyCode, int scanCode) {
        return binding.matches(key(keyCode, scanCode, 0));
    }

    public static int glfwKey(net.minecraft.client.input.KeyEvent e) {
        if (e.isEscape()) return 256;
        if (e.isConfirmation()) return 257;
        int k = e.key();
        if (k == com.mojang.blaze3d.platform.InputConstants.KEY_UP) return 265;
        if (k == com.mojang.blaze3d.platform.InputConstants.KEY_DOWN) return 264;
        if (k == com.mojang.blaze3d.platform.InputConstants.KEY_TAB) return 258;
        return k;
    }

    public static boolean hasControl(int modifiers) {
        return (modifiers & 192) != 0;
    }

    /**
     * 26.3 连鼠标按钮编号也换成了 HID 体系：左键=1、中键=2、右键=3（GLFW 时代
     * 左键=0，26.1/26.2 仍如此）。原版 widget 的 {@code isValidClickButton} 按
     * 新编号判定，而聊天界面全部自绘按钮的命中判断都是 GLFW 风格的
     * {@code button == 0}。此方法把 26.3 的事件按钮值换算回 GLFW 风格。
     */
    //#if MC >= 260300
    public static int glfwButton(int button) {
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return 0;
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE) return 1;
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) return 2;
        return button;
    }

    /** {@link #glfwButton(int)} 的逆换算：把 GLFW 风格按钮还原成 26.3 事件值，
     *  用于构造传回原版 widget 的鼠标事件（widget 按新编号判定）。 */
    public static int rawButton(int button) {
        if (button == 0) return com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
        if (button == 1) return com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE;
        if (button == 2) return com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
        return button;
    }
    //#else
    //$$ public static int glfwButton(int button) {
    //$$     return button;
    //$$ }
    //$$ public static int rawButton(int button) {
    //$$     return button;
    //$$ }
    //#endif

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

    public static boolean mouseClicked(net.minecraft.client.gui.widget.ClickableWidget widget,
                                       double x, double y, int button) {
        return widget.mouseClicked(click(x, y, button), false);
    }

    public static void onClick(net.minecraft.client.gui.widget.ClickableWidget widget,
                               double x, double y) {
        widget.onClick(click(x, y, 0), false);
    }

    public static boolean hasShiftDown() {
        return net.minecraft.client.util.InputUtil.isKeyPressed(
            net.minecraft.client.MinecraftClient.getInstance().getWindow(),
            net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT_SHIFT);
    }
    //#endif
    //#endif
}
