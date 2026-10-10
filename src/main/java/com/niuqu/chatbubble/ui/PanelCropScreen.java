package com.niuqu.chatbubble.ui;

import com.niuqu.chatbubble.render.Appearance;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import com.niuqu.chatbubble.render.PanelBackground;
import com.niuqu.chatbubble.render.RoundRectRenderer;
import com.niuqu.chatbubble.texture.ColoredTextureRenderer;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class PanelCropScreen extends Screen {
    private static final int PAD = 16;
    private static final int BTN_H = 20;
    private static final int BTN_W = 90;
    private static final float MIN_ZOOM = 1f;
    private static final float MAX_ZOOM = 8f;

    private final Screen parent;

    private float centerX, centerY, zoom;

    private int imgX, imgY, dispW, dispH;
    private int btnCancelX, btnConfirmX, btnY;
    private boolean dragging;
    private int lastKnownSize = -1;

    public PanelCropScreen(Screen parent) {
        super(Text.translatable("e33chat.crop.title"));
        this.parent = parent;
        PanelBackground.Crop c = PanelBackground.parseCrop(ChatBubbleClientSetup.config().panelBgCrop());
        this.centerX = c.centerX();
        this.centerY = c.centerY();
        this.zoom = c.zoom();
    }

    @Override
    protected void init() {
        PanelBackground.ensureLoaded();
        lastKnownSize = PanelBackground.imageWidth() * 10000 + PanelBackground.imageHeight();
        layout();
    }

    @Override
    public void tick() {
        if (PanelBackground.available()) {
            int now = PanelBackground.imageWidth() * 10000 + PanelBackground.imageHeight();
            if (now != lastKnownSize) {
                lastKnownSize = now;

                clearChildren();
                init();
            }
        }
        super.tick();
    }

    private void layout() {
        int texW = PanelBackground.imageWidth();
        int texH = PanelBackground.imageHeight();
        int availH = height - PAD * 2 - BTN_H - 24;
        int availW = width - PAD * 2;
        if (texW <= 0 || texH <= 0) {
            dispW = dispH = 0;
            imgX = imgY = 0;
        } else {
            float scale = Math.min((float) availW / texW, (float) availH / texH);

            scale = Math.min(scale, 1f);
            dispW = Math.max(1, Math.round(texW * scale));
            dispH = Math.max(1, Math.round(texH * scale));
            imgX = (width - dispW) / 2;
            imgY = PAD + (availH - dispH) / 2;
        }
        btnY = height - PAD - BTN_H;
        btnConfirmX = width - PAD - BTN_W;
        btnCancelX = btnConfirmX - BTN_W - 8;
    }

    private int[] selectionScreenRect() {
        int texW = PanelBackground.imageWidth();
        int texH = PanelBackground.imageHeight();
        if (texW <= 0 || texH <= 0 || dispW <= 0) return null;
        float aspect = PanelBackground.lastTargetAspect();

        int targetW = Math.max(1, Math.round(aspect * 10000f));
        int targetH = 10000;
        int[] src = PanelBackground.sourceRect(texW, texH, targetW, targetH,
            new PanelBackground.Crop(centerX, centerY, zoom));
        float scale = (float) dispW / texW;
        int sx = imgX + Math.round(src[0] * scale);
        int sy = imgY + Math.round(src[1] * scale);
        int sw = Math.max(1, Math.round(src[2] * scale));
        int sh = Math.max(1, Math.round(src[3] * scale));
        return new int[]{sx, sy, sw, sh};
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

        Identifier tex = PanelBackground.textureId();
        if (tex != null && dispW > 0) {
            ColoredTextureRenderer.drawWithAlpha(g, tex, imgX, imgY, dispW, dispH,
                0f, 0f, PanelBackground.imageWidth(), PanelBackground.imageHeight(),
                PanelBackground.imageWidth(), PanelBackground.imageHeight(), 1f);
        } else {
            String reason = PanelBackground.failed() ? "e33chat.crop.failed"
                : PanelBackground.loading() ? "e33chat.crop.loading" : "e33chat.crop.no_image";
            String msg = Text.translatable(reason).getString();
            g.drawText(textRenderer, msg, (width - textRenderer.getWidth(msg)) / 2, height / 2,
                PanelBackground.failed() ? 0xFFFF6666 : c.textSecondary(), false);
        }

        int[] sel = selectionScreenRect();
        if (sel != null) {
            int dim = 0x99000000;
            g.fill(imgX, imgY, imgX + dispW, sel[1], dim);
            g.fill(imgX, sel[1] + sel[3], imgX + dispW, imgY + dispH, dim);
            g.fill(imgX, sel[1], sel[0], sel[1] + sel[3], dim);
            g.fill(sel[0] + sel[2], sel[1], imgX + dispW, sel[1] + sel[3], dim);

            int border = 0xFFFFFFFF;
            g.fill(sel[0], sel[1], sel[0] + sel[2], sel[1] + 1, border);
            g.fill(sel[0], sel[1] + sel[3] - 1, sel[0] + sel[2], sel[1] + sel[3], border);
            g.fill(sel[0], sel[1], sel[0] + 1, sel[1] + sel[3], border);
            g.fill(sel[0] + sel[2] - 1, sel[1], sel[0] + sel[2], sel[1] + sel[3], border);
        }

        String hint = Text.translatable("e33chat.crop.hint").getString();
        g.drawText(textRenderer, hint, PAD, btnY + 6, c.textSecondary(), false);

        boolean hoverCancel = over(mouseX, mouseY, btnCancelX, btnY, BTN_W, BTN_H);
        boolean hoverConfirm = over(mouseX, mouseY, btnConfirmX, btnY, BTN_W, BTN_H);
        RoundRectRenderer.fill(g, btnCancelX, btnY, btnCancelX + BTN_W, btnY + BTN_H, 4,
            hoverCancel ? 0xFF4A4A52 : 0xFF36363E);
        RoundRectRenderer.fill(g, btnConfirmX, btnY, btnConfirmX + BTN_W, btnY + BTN_H, 4,
            hoverConfirm ? 0xFF3A5FCD : 0xFF2C4A9E);
        String cancelLabel = Text.translatable("gui.cancel").getString();
        String confirmLabel = Text.translatable("gui.done").getString();
        g.drawText(textRenderer, cancelLabel,
            btnCancelX + (BTN_W - textRenderer.getWidth(cancelLabel)) / 2, btnY + 6, 0xFFFFFFFF, false);
        g.drawText(textRenderer, confirmLabel,
            btnConfirmX + (BTN_W - textRenderer.getWidth(confirmLabel)) / 2, btnY + 6, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //#if MC >= 260300
        button = com.niuqu.chatbubble.compat.InputCompat.glfwButton(button);
        //#endif
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (over(mouseX, mouseY, btnCancelX, btnY, BTN_W, BTN_H)) {
            close();
            return true;
        }
        if (over(mouseX, mouseY, btnConfirmX, btnY, BTN_W, BTN_H)) {
            ChatBubbleClientSetup.saveConfig(ChatBubbleClientSetup.config().withPanelBgCrop(
                PanelBackground.formatCrop(new PanelBackground.Crop(centerX, centerY, zoom))));
            client.setScreen(parent);
            return true;
        }
        int[] sel = selectionScreenRect();
        if (sel != null && over(mouseX, mouseY, sel[0], sel[1], sel[2], sel[3])) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && dispW > 0 && dispH > 0) {
            centerX = MathHelper.clamp(centerX + (float) dragX / dispW, 0f, 1f);
            centerY = MathHelper.clamp(centerY + (float) dragY / dispH, 0f, 1f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    //#if MC >= 12002
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            zoom = MathHelper.clamp(zoom * (1f + 0.12f * (float) scrollY), MIN_ZOOM, MAX_ZOOM);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //#else
    //$$ @Override
    //$$ public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
    //$$     if (scrollY != 0) {
    //$$         zoom = MathHelper.clamp(zoom * (1f + 0.12f * (float) scrollY), MIN_ZOOM, MAX_ZOOM);
    //$$         return true;
    //$$     }
    //$$     return super.mouseScrolled(mouseX, mouseY, scrollY);
    //$$ }
    //#endif

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
