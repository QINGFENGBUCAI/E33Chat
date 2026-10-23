package com.niuqu.chatbubble.compat;

public final class InputCompat {
    private InputCompat() {}

    public static int glfwButton(int button) {
        //#if MC >= 260300
        if (button == 1) return 0;
        if (button == 2) return 1;
        if (button == 3) return 2;
        //#endif
        return button;
    }

    public static int rawButton(int button) {
        //#if MC >= 260300
        if (button == 0) return 1;
        if (button == 1) return 2;
        if (button == 2) return 3;
        //#endif
        return button;
    }

    //#if MC >= 12109
    //#if MC >= 26000
    public static net.minecraft.client.gui.Click click(double x, double y, int button) {
        return new net.minecraft.client.gui.Click(x, y,
            new net.minecraft.client.input.MouseInput(rawButton(button), 0));
    }

    public static net.minecraft.client.input.KeyInput key(int keyCode, int scanCode, int modifiers) {
        return new net.minecraft.client.input.KeyInput(rawKey(keyCode), scanCode, modifiers);
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
        if (k == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT) return 263;
        if (k == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT) return 262;
        if (k == 6) return 67;
        if (k == 25) return 86;
        return k;
    }

    public static int rawKey(int keyCode) {
        if (keyCode == 256) return com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE;
        if (keyCode == 257) return 40;
        if (keyCode == 265) return com.mojang.blaze3d.platform.InputConstants.KEY_UP;
        if (keyCode == 264) return com.mojang.blaze3d.platform.InputConstants.KEY_DOWN;
        if (keyCode == 258) return com.mojang.blaze3d.platform.InputConstants.KEY_TAB;
        return keyCode;
    }

    public static boolean hasControl(int modifiers) {
        return (modifiers & 192) != 0;
    }

    public static boolean hasShiftDown() {
        return net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown();
    }
    //#else
    //$$ public static net.minecraft.client.gui.Click click(double x, double y, int button) {
    //$$     return new net.minecraft.client.gui.Click(x, y,
    //$$         new net.minecraft.client.input.MouseInput(button, 0));
    //$$ }
    //$$ 
    //$$ public static net.minecraft.client.input.KeyInput key(int keyCode, int scanCode, int modifiers) {
    //$$     return new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers);
    //$$ }
    //$$ 
    //$$ public static boolean mouseClicked(net.minecraft.client.gui.widget.ClickableWidget widget,
    //$$                                        double x, double y, int button) {
    //$$     return widget.mouseClicked(click(x, y, button), false);
    //$$ }
    //$$ 
    //$$ public static void onClick(net.minecraft.client.gui.widget.ClickableWidget widget,
    //$$                            double x, double y) {
    //$$     widget.onClick(click(x, y, 0), false);
    //$$ }
    //$$ 
    //$$ public static boolean hasShiftDown() {
    //$$     return net.minecraft.client.util.InputUtil.isKeyPressed(
    //$$         net.minecraft.client.MinecraftClient.getInstance().getWindow(),
    //$$         net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT_SHIFT);
    //$$ }
    //$$ 
    //$$ public static boolean hasControl(int modifiers) {
    //$$     return (modifiers & 0x2) != 0;
    //$$ }
    //#endif
    //#endif
}
