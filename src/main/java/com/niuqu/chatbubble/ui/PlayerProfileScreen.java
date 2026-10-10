package com.niuqu.chatbubble.ui;

import com.niuqu.chatbubble.render.Appearance;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import com.niuqu.chatbubble.render.RoundRectRenderer;
import com.niuqu.chatbubble.render.SkinResolver;
import net.minecraft.client.MinecraftClient;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
//#if MC >= 26000
import net.minecraft.world.level.GameType;
//#else
//$$ import net.minecraft.world.GameMode;
//#endif

import java.util.UUID;

public class PlayerProfileScreen extends Screen {

    private static final int PANEL_W = 220;
    private static final int PANEL_H = 196;
    private static final int BTN_H = 16;

    private final Screen parent;
    private final String playerName;

    private int panelX, panelY;
    private int btnWhisperX, btnCopyX, btnY, btnW;

    public PlayerProfileScreen(Screen parent, String playerName) {
        super(Text.translatable("e33chat.profile.title"));
        this.parent = parent;
        this.playerName = playerName;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        btnW = (PANEL_W - 24 - 8) / 2;
        btnWhisperX = panelX + 12;
        btnCopyX = btnWhisperX + btnW + 8;
        btnY = panelY + PANEL_H - 12 - BTN_H;
    }

    private PlayerListEntry info() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;
        PlayerListEntry exact = mc.getNetworkHandler().getPlayerListEntry(playerName);
        if (exact != null) return exact;
        for (PlayerListEntry p : mc.getNetworkHandler().getPlayerList()) {
            if (p.getProfile().getName().equalsIgnoreCase(playerName)) return p;
        }
        return null;
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        //#if MC >= 26000

        //#else
        //#if MC >= 12002
        renderBackground(g, mouseX, mouseY, partialTick);
        //#else
        //$$ renderBackground(g);
        //#endif
        //#endif
        ChatBubbleTheme.Colors c = Appearance.snapshot();

        RoundRectRenderer.fill(g, panelX + 2, panelY + 3, panelX + PANEL_W + 2, panelY + PANEL_H + 3,
            8, 0x55000000);
        RoundRectRenderer.fill(g, panelX, panelY, panelX + PANEL_W, panelY + PANEL_H,
            8, 0xF21A1C20);

        PlayerListEntry info = info();
        boolean online = info != null;
        boolean isSelf = MinecraftClient.getInstance().player != null
            && MinecraftClient.getInstance().player.getName().getString().equalsIgnoreCase(playerName);
        UUID uuid = online ? info.getProfile().getId() : null;

        int headS = 40;
        int headX = panelX + (PANEL_W - headS) / 2;
        int headY = panelY + 14;
        SkinResolver.drawAvatar(g, uuid, playerName, headX, headY, headS, headS + 6, 1f, !isSelf && !online);

        int nameW = textRenderer.getWidth(playerName);
        String badge = Text.translatable(isSelf ? "e33chat.profile.self"
            : online ? "e33chat.profile.online" : "e33chat.profile.offline").getString();
        int badgeColor = isSelf ? 0xFF55FFFF : online ? 0xFF55FF55 : 0xFF888888;
        int totalW = nameW + 6 + textRenderer.getWidth(badge);
        int nameX = panelX + (PANEL_W - totalW) / 2;
        int nameY = headY + headS + 8;
        g.drawText(textRenderer, playerName, nameX, nameY, c.textPrimary(), false);
        g.drawText(textRenderer, badge, nameX + nameW + 6, nameY, badgeColor, false);

        int fieldX = panelX + 16;
        int fieldY = nameY + 18;
        int lineH = textRenderer.fontHeight + 4;
        fieldY = drawField(g, Text.translatable("e33chat.profile.uuid").getString(),
            uuid != null ? uuid.toString() : "—", fieldX, fieldY, lineH, c);
        fieldY = drawField(g, Text.translatable("e33chat.profile.latency").getString(),
            online ? info.getLatency() + " ms" : "—", fieldX, fieldY, lineH, c);
        GameMode gt = online ? info.getGameMode() : null;

        //#if MC >= 12105
        String modeName = gt != null ? gt.getTranslatableName().getString() : "—";
        //#else
        //$$ String modeName = gt != null ? gt.getName() : "—";
        //#endif
        drawField(g, Text.translatable("e33chat.profile.gamemode").getString(),
            modeName, fieldX, fieldY, lineH, c);

        boolean hoverW = over(mouseX, mouseY, btnWhisperX, btnY, btnW, BTN_H);
        boolean hoverC = over(mouseX, mouseY, btnCopyX, btnY, btnW, BTN_H);
        RoundRectRenderer.fill(g, btnWhisperX, btnY, btnWhisperX + btnW, btnY + BTN_H, 4,
            hoverW ? 0xFF3A5FCD : 0xFF2C4A9E);
        RoundRectRenderer.fill(g, btnCopyX, btnY, btnCopyX + btnW, btnY + BTN_H, 4,
            hoverC ? 0xFF4A4A52 : 0xFF36363E);
        String whisperLabel = Text.translatable("e33chat.context.whisper").getString();
        String copyLabel = Text.translatable("e33chat.profile.copy_uuid").getString();
        g.drawText(textRenderer, whisperLabel,
            btnWhisperX + (btnW - textRenderer.getWidth(whisperLabel)) / 2, btnY + 4, 0xFFFFFFFF, false);
        g.drawText(textRenderer, copyLabel,
            btnCopyX + (btnW - textRenderer.getWidth(copyLabel)) / 2, btnY + 4, 0xFFFFFFFF, false);
    }

    private int drawField(DrawContext g, String label, String value, int x, int y, int lineH,
                          ChatBubbleTheme.Colors c) {
        g.drawText(textRenderer, label, x, y, c.textSecondary(), false);
        String v = value;
        int maxW = PANEL_W - 32 - 60;
        if (textRenderer.getWidth(v) > maxW) v = textRenderer.trimToWidth(v, maxW - textRenderer.getWidth("…")) + "…";
        g.drawText(textRenderer, v, x + 60, y, c.textPrimary(), false);
        return y + lineH;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //#if MC >= 260300
        button = com.niuqu.chatbubble.compat.InputCompat.glfwButton(button);
        //#endif
        if (button == 0) {
            if (over(mouseX, mouseY, btnWhisperX, btnY, btnW, BTN_H)) {
                MinecraftClient mc = MinecraftClient.getInstance();
                close();
                if (mc.player != null) mc.player.networkHandler.sendChatCommand("msg " + playerName + " ");
                return true;
            }
            if (over(mouseX, mouseY, btnCopyX, btnY, btnW, BTN_H)) {
                PlayerListEntry info = info();
                String text = info != null ? info.getProfile().getId().toString() : playerName;
                MinecraftClient.getInstance().keyboard.setClipboard(text);
                return true;
            }

            if (!over(mouseX, mouseY, panelX, panelY, PANEL_W, PANEL_H)) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
