package com.niuqu.chatbubble.ui;
import com.niuqu.chatbubble.ChatBubbleScreen;

//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;

// 睡眠状态界面
public class BedScreen extends Screen {
    private static Screen screenBeforeSleep;

    // 构造睡眠界面
    public BedScreen() {
        super(Text.translatable("multiplayer.stopSleeping"));
    }

    // 记录睡前界面
    public static void setScreenBeforeSleep(Screen screen) {
        screenBeforeSleep = screen;
    }

    // 初始化界面控件
    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("multiplayer.stopSleeping"), b -> sendWakeUp())
            .dimensions(width / 2 - 100, height - 40, 200, 20).build());
    }

    //#if MC >= 12002
    // 绘制背景
    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
    }
    //#else
    //$$ @Override
    //$$ public void renderBackground(DrawContext g) {
    //$$ }
    //#endif

    // 检测睡眠状态
    @Override
    public void tick() {
        if (client == null || client.player == null || !client.player.isSleeping()) {
            client.setScreen(null);
            if (screenBeforeSleep instanceof ChatBubbleScreen) {
                client.setScreen(screenBeforeSleep);
            }
            screenBeforeSleep = null;
        }
    }

    // 处理按键事件
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        //#if MC >= 26000
        if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
        //#else
        //$$ if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
        //#endif
            sendWakeUp();
            return true;
        }
        if (client.options.chatKey.matchesKey(keyCode, scanCode)) {
            client.setScreen(new ChatBubbleScreen(""));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // 是否暂停游戏
    @Override
    public boolean shouldPause() {
        return false;
    }

    // 发送起床请求
    private void sendWakeUp() {
        if (client != null && client.player != null) {
            client.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.STOP_SLEEPING));
        }
    }
}
