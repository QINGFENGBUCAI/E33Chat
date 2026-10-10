package com.niuqu.chatbubble.config;
import com.niuqu.chatbubble.store.ChatMessageStore;

import com.niuqu.chatbubble.chat.TemplateMatcher;
import com.niuqu.chatbubble.network.ServerConfigSavePayload;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import com.niuqu.chatbubble.texture.ColoredTextureRenderer;
import com.niuqu.chatbubble.texture.UiElement;
import com.niuqu.chatbubble.texture.UiTextureManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#if MC < 12005
//$$ import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//#endif
import net.minecraft.client.MinecraftClient;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class ServerConfigScreen extends Screen {
    private final Screen lastScreen;

    private boolean hudHidden;

    private static final int ROW_H = 32;
    private static final int START_Y = 40;
    private static final int CAT_X = 24;
    private static final int CAT_W = 96;
    private static final int CAT_ROW_H = 22;
    private static final int INPUT_W = 170;

    private static final int TEMPLATE_INPUT_W = 170;

    private static final int BUTTON_W = 90;
    private static final int SCROLLBAR_W = 6;

    private static final String[] CHAT_PRESETS = {
        "{display_name}{sep}{content}",
        "<{display_name}> {content}",
        "[{display_name}]: {content}",
        "{prefix}{display_name}{sep}{content}",
        "&7[{group}]&r {display_name}&7:&r {content}",
        "[Guest] {display_name} > {content}",
        "{display_name} >> {content}",
        "-{display_name}- {content}",
        "【{display_name}】{content}",
        "[{prefix}] <{external}> {content}",
        "<{external}> {content}",
    };
    private static final String[] WHISPER_PRESETS = {
        "{sender}悄悄地对你说{sep}{content}",
        "{sender} whispered to you{sep}{content}",
        "[/msg from {sender}] {content}",
        "{sender} -> {target}{sep}{content}",
        "[私聊] {sender}{sep}{content}",
        "{sender}私聊 {target}{sep}{content}",
    };

    private static final String[] CAT_KEYS = {
        "e33chat.server.cat.general",
        "e33chat.server.cat.chat",
        "e33chat.server.cat.whisper",
        "e33chat.server.cat.debug",
        "e33chat.server.cat.tutorial",
    };

    private final boolean initUseTpa, initHistory, initDebug, initMedia, initAutoClean, initEasyBot, initGroups;
    private boolean useTpaV, historyV, debugV, mediaV, autoCleanV, easyBotV, groupsV;
    private final List<String> initChat, initWhisper;
    private final List<String> chatV = new ArrayList<>();
    private final List<String> whisperV = new ArrayList<>();
    private boolean genVisible;
    private String genText = "";
    private String error;

    private String genError;
    private Row genInputRow;
    private String previewChatResult = "", previewWhisperResult = "";

    private final java.util.Map<TextFieldWidget, String> boxErrors = new java.util.LinkedHashMap<>();

    private String pendingPreviewText;

    private int selectedCat;
    private final com.niuqu.chatbubble.render.SmoothScrollPane rightPane = new com.niuqu.chatbubble.render.SmoothScrollPane();
    private final com.niuqu.chatbubble.render.SmoothScrollPane treePane = new com.niuqu.chatbubble.render.SmoothScrollPane();

    private record Row(Text label, List<Element> widgets,
                       String extraText, int height, String tooltipKey, boolean title) {}
    private final List<Row> rows = new ArrayList<>();
    private ButtonWidget doneBtn, exitBtn, saveBtn;

    public ServerConfigScreen(Screen lastScreen, boolean useTpa, boolean history, boolean debug,
                              boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
                              boolean groupsEnabled,
                              List<String> chat, List<String> whisper) {
        super(Text.translatable("e33chat.server.title"));
        this.lastScreen = lastScreen;
        initUseTpa = useTpa;
        initHistory = history;
        initDebug = debug;
        initMedia = mediaEnabled;
        initAutoClean = mediaAutoClean;
        initEasyBot = easyBotCompat;
        initGroups = groupsEnabled;
        initChat = new ArrayList<>(chat);
        initWhisper = new ArrayList<>(whisper);
        useTpaV = useTpa;
        historyV = history;
        debugV = debug;
        mediaV = mediaEnabled;
        autoCleanV = mediaAutoClean;
        easyBotV = easyBotCompat;
        groupsV = groupsEnabled;
        chatV.addAll(chat);
        whisperV.addAll(whisper);
    }

    private ChatBubbleTheme.Colors c() { return ChatBubbleTheme.DARK.colors(); }

    private int dividerX() { return CAT_X + CAT_W + 12; }
    private int optLabelX() { return dividerX() + 14; }
    private int previewX() { return width - 26; }
    private int inputX() { return previewX() - 8 - INPUT_W; }
    private int optAreaW() { return previewX() - optLabelX() - 4; }

    private int rightAreaW() { return previewX() - 8 - optLabelX(); }

    private int btnRight() { return previewX() - 8 - BUTTON_W; }
    private int btnLeft() { return btnRight() - 4 - BUTTON_W; }
    private int viewTop() { return START_Y; }
    private int viewBottom() { return height - 40; }

    private int totalRowsH() {
        int total = 0;
        for (Row r : rows) total += r.height();
        return total;
    }

    private int calcMaxScroll() {
        return Math.max(0, viewTop() + totalRowsH() - viewBottom());
    }

    private int calcTreeMaxScroll() {
        return Math.max(0, START_Y + CAT_ROW_H * CAT_KEYS.length - viewBottom());
    }

    private <T extends net.minecraft.client.gui.widget.ClickableWidget> T reg(T w) {
        return addDrawableChild(w);
    }

    private Row row(Text label, List<Element> widgets, String extraText, String tooltipKey) {
        for (Element w : widgets) {
            if (w instanceof net.minecraft.client.gui.widget.ClickableWidget cw) addDrawableChild(cw);
        }
        return new Row(label, widgets, extraText, ROW_H, tooltipKey, false);
    }

    private Row textRow(Text label, int height) {
        return new Row(label, List.of(), null, height, null, false);
    }

    private Row titleRow(Text label) {
        return new Row(label, List.of(), null, ROW_H, null, true);
    }

    private void buildRows() {
        rows.clear();
        switch (selectedCat) {
            case 0 -> {
                rows.add(row(Text.translatable("e33chat.server.use_tpa"),
                    List.of(mkToggle(() -> useTpaV, nv -> useTpaV = nv)), null, "e33chat.server.use_tpa"));
                rows.add(row(Text.translatable("e33chat.server.history"),
                    List.of(mkToggle(() -> historyV, nv -> historyV = nv)), null, "e33chat.server.history"));
                rows.add(row(Text.translatable("e33chat.server.media_enabled"),
                    List.of(mkToggle(() -> mediaV, nv -> mediaV = nv)), null, "e33chat.server.media_enabled"));
                rows.add(row(Text.translatable("e33chat.server.media_auto_clean"),
                    List.of(mkToggle(() -> autoCleanV, nv -> autoCleanV = nv)), null, "e33chat.server.media_auto_clean"));
                rows.add(row(Text.translatable("e33chat.server.easybot_compat"),
                    List.of(mkToggle(() -> easyBotV, nv -> easyBotV = nv)), null, "e33chat.server.easybot_compat"));
                rows.add(row(Text.translatable("e33chat.server.groups_enabled"),
                    List.of(mkToggle(() -> groupsV, nv -> groupsV = nv)), null, "e33chat.server.groups_enabled"));
            }
            case 1 -> buildTemplateRows(chatV, true);
            case 2 -> buildTemplateRows(whisperV, false);
            case 3 -> rows.add(row(Text.translatable("e33chat.server.template_debug"),
                List.of(mkToggle(() -> debugV, nv -> debugV = nv)), null, "e33chat.server.template_debug"));
            case 4 -> buildTutorialRows();
        }
    }

    private void buildTemplateRows(List<String> list, boolean chat) {

        for (int i = 0; i < list.size(); i++) {
            int idx = i;
            Text label = Text.translatable("e33chat.server.template_n", i + 1);
            TextFieldWidget box = mkBox(previewX() - 8 - TEMPLATE_INPUT_W - 24, TEMPLATE_INPUT_W);
            box.setText(list.get(idx));
            box.setChangedListener(s -> {
                if (idx < list.size()) list.set(idx, s);

                var r = TemplateMatcher.compile(s);
                if (r.template() == null) boxErrors.put(box, r.error());
                else boxErrors.remove(box);
            });
            ButtonWidget rm = ButtonWidget.builder(Text.literal("✕"), b -> { list.remove(idx); rebuild(); })
                .dimensions(previewX() - 8 - 20, 0, 20, 20).build();
            rows.add(new Row(label, List.of(reg(rm), reg(box)), null, ROW_H,
                "e33chat.server.template_n", false));
        }

        ButtonWidget add = ButtonWidget.builder(Text.translatable("e33chat.server.add"), b -> { list.add(""); rebuild(); })
            .dimensions(btnLeft(), 0, BUTTON_W, 20).build();
        if (chat) {
            ButtonWidget genOpen = ButtonWidget.builder(Text.translatable("e33chat.server.gen_open"),
                b -> { genVisible = true; rebuild(); }).dimensions(btnRight(), 0, BUTTON_W, 20).build();
            rows.add(row(Text.translatable("e33chat.server.actions"), List.of(add, genOpen), null, "e33chat.server.actions"));
            if (genVisible) {
                TextFieldWidget genBox = mkBox(inputX(), INPUT_W);
                genBox.setText(genText);
                genBox.setChangedListener(s -> { genText = s; genError = null; });
                rows.add(genInputRow = row(Text.translatable("e33chat.server.gen"), List.of(genBox), null, "e33chat.server.gen"));
                ButtonWidget genOk = ButtonWidget.builder(Text.translatable("e33chat.server.gen_confirm"),
                    b -> generateFromMessage()).dimensions(btnLeft(), 0, BUTTON_W, 20).build();
                ButtonWidget genCancel = ButtonWidget.builder(Text.translatable("e33chat.server.gen_cancel"),
                    b -> { genVisible = false; genError = null; rebuild(); }).dimensions(btnRight(), 0, BUTTON_W, 20).build();
                rows.add(row(Text.translatable("e33chat.server.gen"), List.of(genOk, genCancel), null, null));
            }
        } else {
            rows.add(row(Text.translatable("e33chat.server.actions"), List.of(add), null, "e33chat.server.actions"));
        }

        String[] presets = chat ? CHAT_PRESETS : WHISPER_PRESETS;
        rows.add(titleRow(Text.translatable("e33chat.server.preset_section")));
        for (String p : presets) {
            ButtonWidget pb = ButtonWidget.builder(Text.literal("+"),
                b -> { if (!list.contains(p)) { list.add(p); rebuild(); } })
                .dimensions(inputX() + INPUT_W - 20, 0, 20, 20).build();
            Text label = Text.literal(truncate(p, inputX() - optLabelX() - 8));
            rows.add(new Row(label, List.of(reg(pb)), null, ROW_H, null, false));
        }

        TextFieldWidget preview = mkBox(inputX(), INPUT_W);
        preview.setChangedListener(s -> {
            if (chat) previewChatResult = runPreview(s, chatV, false);
            else previewWhisperResult = runPreview(s, whisperV, true);
        });
        if (pendingPreviewText != null) {
            preview.setText(pendingPreviewText);
            pendingPreviewText = null;
        }
        rows.add(row(Text.translatable("e33chat.server.preview"), List.of(preview),
            chat ? previewChatResult : previewWhisperResult, "e33chat.server.preview"));
    }

    private void buildTutorialRows() {
        for (String key : List.of("quick", "concept", "fields", "faq", "why")) {
            rows.add(titleRow(Text.translatable("e33chat.tutorial." + key + ".title")));
            for (String para : Text.translatable("e33chat.tutorial." + key).getString().split("\n")) {
                if (para.isBlank()) {
                    rows.add(textRow(Text.literal(""), 6));
                    continue;
                }
                for (String line : wrapText(para)) {
                    rows.add(textRow(Text.literal(line), 14));
                }
            }
            rows.add(textRow(Text.literal(""), 10));
        }
    }

    private List<String> wrapText(String raw) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int maxW = optAreaW();
        int lastSpace = -1;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == ' ') lastSpace = cur.length();
            cur.append(ch);
            if (textRenderer.getWidth(cur.toString()) > maxW) {
                int cut = lastSpace > 0 ? lastSpace : cur.length() - 1;
                if (cut <= 0) cut = cur.length() - 1;
                String line = cur.substring(0, cut).trim();
                if (!line.isEmpty()) out.add(line);
                cur.delete(0, cut);
                lastSpace = -1;
            }
        }
        if (cur.length() > 0) {
            String line = cur.toString().trim();
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    private String truncate(String s, int maxWidth) {
        if (textRenderer.getWidth(s) <= maxWidth) return s;
        String cut = textRenderer.trimToWidth(s, maxWidth - 6);
        return cut + "…";
    }

    private void generateFromMessage() {
        String inferred = TemplateMatcher.inferFromMessage(genText, knownNames()).orElse(null);
        if (inferred == null) {

            genError = Text.translatable("e33chat.server.gen_failed").getString()
                + "  " + Text.translatable("e33chat.server.gen_howto").getString();
            return;
        }
        chatV.add(inferred);
        genVisible = false;
        genError = null;

        pendingPreviewText = genText;
        genText = "";
        rebuild();
    }

    private List<String> knownNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        var player = MinecraftClient.getInstance().player;
        if (player != null && player.networkHandler != null) {
            for (var info : player.networkHandler.getPlayerList()) {
                names.add(info.getProfile().getName());
            }
        }
        names.addAll(ChatMessageStore.knownNameVariants());
        return new ArrayList<>(names);
    }

    private String runPreview(String text, List<String> raws, boolean whisper) {
        if (text == null || text.isBlank()) return "";
        List<TemplateMatcher.CompiledTemplate> tpls = new ArrayList<>();
        for (String raw : raws) {
            var r = TemplateMatcher.compile(raw);
            if (r.template() != null) tpls.add(r.template());
        }
        var m = TemplateMatcher.match(text, whisper ? List.of() : tpls, whisper ? tpls : List.of(),
            ChatMessageStore::isKnownPlayerName);
        if (m.isEmpty()) return Text.translatable("e33chat.server.preview_miss").getString();
        var t = m.orElseThrow();
        String name = whisper && t.sender() != null ? t.sender() : t.displayName();
        return Text.translatable("e33chat.server.preview_hit", name, t.content()).getString();
    }

    private ButtonWidget mkToggle(java.util.function.BooleanSupplier current,
                                  java.util.function.Consumer<Boolean> apply) {
        return ButtonWidget.builder(current.getAsBoolean() ? ScreenTexts.ON : ScreenTexts.OFF,
            b -> {
                boolean nv = !current.getAsBoolean();
                apply.accept(nv);
                b.setMessage(nv ? ScreenTexts.ON : ScreenTexts.OFF);
            }).dimensions(btnRight(), 0, BUTTON_W, 20).build();
    }

    private TextFieldWidget mkBox(int x, int w) {
        TextFieldWidget box = new TextFieldWidget(textRenderer, x, 0, w, 20, Text.literal(""));
        box.setMaxLength(200);
        return box;
    }

    private boolean changed() {
        return useTpaV != initUseTpa || historyV != initHistory || debugV != initDebug
            || mediaV != initMedia || autoCleanV != initAutoClean || easyBotV != initEasyBot
            || groupsV != initGroups
            || !Objects.equals(chatV, initChat) || !Objects.equals(whisperV, initWhisper);
    }

    private void save() {
        String err = validate("chat", chatV);
        if (err == null) err = validate("whisper", whisperV);
        if (err != null) {
            error = err;
            return;
        }
        //#if MC >= 12005
        ClientPlayNetworking.send(new ServerConfigSavePayload(
            useTpaV, historyV, debugV, mediaV, autoCleanV, easyBotV, groupsV,
            new ArrayList<>(chatV), new ArrayList<>(whisperV)));
        //#else
        //$$ ServerConfigSavePayload p = new ServerConfigSavePayload(
        //$$     useTpaV, historyV, debugV, mediaV, autoCleanV, easyBotV, groupsV,
        //$$     new ArrayList<>(chatV), new ArrayList<>(whisperV));
        //$$ ClientPlayNetworking.send(ServerConfigSavePayload.ID, p.write(PacketByteBufs.create()));
        //#endif
        doClose();
    }

    private static String validate(String kind, List<String> templates) {
        for (int i = 0; i < templates.size(); i++) {
            TemplateMatcher.CompileResult result = TemplateMatcher.compile(templates.get(i));
            if (result.template() == null) {
                return Text.translatable("e33chat.server.invalid",
                    Text.translatable("e33chat.server." + kind), i + 1, result.error()).getString();
            }
        }
        return null;
    }

    private void doClose() {
        if (client != null) client.setScreen(lastScreen);
    }

    @Override
    public void removed() {
        if (hudHidden) {
            com.niuqu.chatbubble.render.HudVisibility.pop();
            hudHidden = false;
        }
        super.removed();
    }

    @Override
    public void close() {
        if (changed()) {
            client.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) doClose();
                else client.setScreen(this);
            },
                Text.translatable("e33chat.config.discard.title"),
                Text.translatable("e33chat.config.discard.message", changeCount())));
        } else {
            doClose();
        }
    }

    private int changeCount() {
        int n = 0;
        if (useTpaV != initUseTpa) n++;
        if (historyV != initHistory) n++;
        if (debugV != initDebug) n++;
        if (mediaV != initMedia) n++;
        if (autoCleanV != initAutoClean) n++;
        if (easyBotV != initEasyBot) n++;
        if (groupsV != initGroups) n++;
        if (!Objects.equals(chatV, initChat)) n++;
        if (!Objects.equals(whisperV, initWhisper)) n++;
        return n;
    }

    @Override
    protected void init() {

        if (!hudHidden) {
            com.niuqu.chatbubble.render.HudVisibility.push();
            hudHidden = true;
        }
        buildRows();
        rightPane.setOffset(MathHelper.clamp(rightPane.offset(), 0, calcMaxScroll()));
        treePane.setOffset(MathHelper.clamp(treePane.offset(), 0, calcTreeMaxScroll()));

        doneBtn = addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> doClose())
            .dimensions(width / 2 - 100, height - 32, 200, 20).build());
        exitBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("e33chat.config.exit"), b -> doClose())
            .dimensions(width / 2 - 104, height - 32, 100, 20).build());
        saveBtn = addDrawableChild(ButtonWidget.builder(Text.translatable("e33chat.server.save"), b -> save())
            .dimensions(width / 2 + 4, height - 32, 100, 20).build());

        relayoutWidgets();
    }

    private void rebuild() {
        rightPane.setOffset(0);
        setFocused(null);
        clearChildren();
        boxErrors.clear();
        init();
    }

    private void switchCategory(int idx) {
        if (idx == selectedCat) return;
        selectedCat = idx;
        rebuild();
    }

    private void relayoutWidgets() {
        int y = viewTop() - rightPane.offset();
        for (Row row : rows) {
            for (Element w : row.widgets()) {
                if (w instanceof net.minecraft.client.gui.widget.ClickableWidget cw) {
                    cw.setY(y);
                    cw.visible = y >= viewTop() && y + row.height() <= viewBottom();
                }
            }
            y += row.height();
        }
    }

    private static int sbThumbH(int trackH, int totalH) {
        return Math.max(8, (int) ((long) trackH * trackH / totalH));
    }
    private static int sbThumbY(int top, int trackH, int th, int offset, int maxScroll) {
        if (maxScroll <= 0 || trackH <= th) return top;
        return top + (int) ((long) (trackH - th) * offset / maxScroll);
    }
    private static boolean sbHovering(int mx, int my, int tx, int ty, int th) {
        return mx >= tx && mx < tx + SCROLLBAR_W && my >= ty && my < ty + th;
    }

    private int rTrackX() { return width - SCROLLBAR_W; }
    private int rTrackH() { return viewBottom() - viewTop(); }
    private int rTotalH() { return calcMaxScroll() + rTrackH(); }
    private int tTrackX() { return dividerX() - SCROLLBAR_W - 2; }
    private int tTrackH() { return viewBottom() - START_Y; }
    private int tTotalH() { return calcTreeMaxScroll() + tTrackH(); }

    private void startR(float target, int dur) {
        rightPane.animateTo(target, calcMaxScroll(), dur);
    }

    private void startT(float target, int dur) {
        treePane.animateTo(target, calcTreeMaxScroll(), dur);
    }

    private void tickAnims() {
        rightPane.tick(calcMaxScroll());
        treePane.tick(calcTreeMaxScroll());
        relayoutWidgets();
    }

    private void drawBar(DrawContext g, int trackX, int top, int bot,
                         int totalH, int offset, int maxScroll,
                         double mx, double my, boolean dragging) {
        if (maxScroll <= 0) return;
        int trackH = bot - top;
        int th = sbThumbH(trackH, totalH);
        int ty = sbThumbY(top, trackH, th, offset, maxScroll);
        ColoredTextureRenderer.drawWithAlpha(g,
            UiTextureManager.rl(UiElement.SCROLLBAR_TRACK, ChatBubbleTheme.DARK),
            trackX, top, SCROLLBAR_W, bot - top, 0x40 / 255f);
        int base = dragging ? 0xCC
            : sbHovering((int) mx, (int) my, trackX, ty, th) ? 0xAA : 0x88;
        ColoredTextureRenderer.drawWithAlpha(g,
            UiTextureManager.rl(UiElement.SCROLLBAR_THUMB, ChatBubbleTheme.DARK),
            trackX, ty, SCROLLBAR_W, th, base / 255f);
    }

    private void drawTriangle(DrawContext g, int x, int y, boolean down, int color) {
        if (down) {
            g.fill(x, y, x + 5, y + 1, color);
            g.fill(x + 1, y + 1, x + 4, y + 2, color);
            g.fill(x + 2, y + 2, x + 3, y + 3, color);
        } else {
            g.fill(x, y, x + 1, y + 1, color);
            g.fill(x, y + 1, x + 2, y + 2, color);
            g.fill(x, y + 2, x + 3, y + 3, color);
            g.fill(x, y + 3, x + 2, y + 4, color);
            g.fill(x, y + 4, x + 1, y + 5, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //#if MC >= 260300
        button = com.niuqu.chatbubble.compat.InputCompat.glfwButton(button);
        //#endif
        int rMax = calcMaxScroll();
        if (rMax > 0 && mouseX >= rTrackX() && mouseX < rTrackX() + SCROLLBAR_W
                && mouseY >= viewTop() && mouseY < viewBottom()) {
            int th = sbThumbH(rTrackH(), rTotalH());
            int ty = sbThumbY(viewTop(), rTrackH(), th, rightPane.offset(), rMax);
            if (mouseY < ty) startR(rightPane.offset() - rTrackH(), 120);
            else if (mouseY > ty + th) startR(rightPane.offset() + rTrackH(), 120);
            else rightPane.dragStart((int) mouseY, rightPane.offset());
            return true;
        }
        int tMax = calcTreeMaxScroll();
        if (tMax > 0 && mouseX >= tTrackX() && mouseX < tTrackX() + SCROLLBAR_W
                && mouseY >= START_Y && mouseY < viewBottom()) {
            int th = sbThumbH(tTrackH(), tTotalH());
            int ty = sbThumbY(START_Y, tTrackH(), th, treePane.offset(), tMax);
            if (mouseY < ty) startT(treePane.offset() - tTrackH(), 120);
            else if (mouseY > ty + th) startT(treePane.offset() + tTrackH(), 120);
            else treePane.dragStart((int) mouseY, treePane.offset());
            return true;
        }
        if (button == 0) {
            int ly = START_Y - treePane.offset();
            for (int i = 0; i < CAT_KEYS.length; i++) {
                if (mouseY >= ly && mouseY < ly + CAT_ROW_H && mouseX >= CAT_X && mouseX <= CAT_X + CAT_W) {
                    switchCategory(i);
                    return true;
                }
                ly += CAT_ROW_H;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    //#if MC >= 12002
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    //#else
    //$$ @Override
    //$$ public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //$$     double horizontalAmount = 0;
    //$$     double verticalAmount = amount;
    //#endif
        if (mouseX < dividerX()) {
            if (calcTreeMaxScroll() <= 0) return false;
            treePane.wheel(verticalAmount, calcTreeMaxScroll(), 120);
            return true;
        }
        if (calcMaxScroll() <= 0) return false;
        rightPane.wheel(verticalAmount, calcMaxScroll(), 120);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (rightPane.dragging()) {
            rightPane.dragTo((int) mouseY, rTrackH(), rTotalH(), calcMaxScroll(), 80);
            return true;
        }
        if (treePane.dragging()) {
            treePane.dragTo((int) mouseY, tTrackH(), tTotalH(), calcTreeMaxScroll(), 80);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        rightPane.dragEnd();
        treePane.dragEnd();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.drawTexture(UiTextureManager.rl(UiElement.CONFIG_BG, ChatBubbleTheme.DARK),
            0, 0, width, height, 0f, 0f, 16, 16, 16, 16);
        tickAnims();
        g.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, 14, c().configTitle(), false);

        String tooltipKey = null;

        g.enableScissor(CAT_X, START_Y, dividerX(), viewBottom());
        int ly = START_Y - treePane.offset();
        for (int i = 0; i < CAT_KEYS.length; i++) {
            boolean sel = i == selectedCat;
            boolean hover = mouseX >= CAT_X && mouseX <= CAT_X + CAT_W && mouseY >= ly && mouseY < ly + CAT_ROW_H;
            if (sel || hover)
                g.drawTexture(UiTextureManager.rl(UiElement.HOVER_BG, ChatBubbleTheme.DARK),
                    CAT_X, ly, CAT_W, CAT_ROW_H, 0f, 0f, 16, 16, 16, 16);
            if (sel)
                g.fill(CAT_X, ly, CAT_X + 2, ly + CAT_ROW_H, c().configTitle());
            g.drawText(textRenderer, Text.translatable(CAT_KEYS[i]), CAT_X + 18, ly + (CAT_ROW_H - 8) / 2,
                sel ? c().configTitle() : c().configLabel(), false);
            ly += CAT_ROW_H;
        }
        g.disableScissor();
        drawBar(g, tTrackX(), START_Y, viewBottom(), tTotalH(), treePane.offset(), calcTreeMaxScroll(),
            mouseX, mouseY, treePane.dragging());

        g.drawTexture(UiTextureManager.rl(UiElement.DIVIDER, ChatBubbleTheme.DARK),
            dividerX(), START_Y - 6, 1, viewBottom() - (START_Y - 6), 0f, 0f, 16, 16, 16, 16);

        g.enableScissor(optLabelX() - 4, viewTop(), width, viewBottom());
        int y = viewTop() - rightPane.offset();
        for (Row row : rows) {
            if (row.title()) {

                Text label = row.label();
                g.drawText(textRenderer, label, optLabelX(), y + 11, c().configLabel(), false);
                int lineX = optLabelX() + textRenderer.getWidth(label) + 8;
                int lineEnd = optLabelX() + optAreaW() + 4;
                if (lineX < lineEnd)
                    g.drawTexture(UiTextureManager.rl(UiElement.DIVIDER, ChatBubbleTheme.DARK),
                        lineX, y + 15, lineEnd - lineX, 1, 0f, 0f, 16, 16, 16, 16);
                y += row.height();
                continue;
            }
            if (!row.label().getString().isEmpty()) {
                int labelY = row.height() == ROW_H ? y + 6 : y + 2;
                g.drawText(textRenderer, row.label(), optLabelX(), labelY, c().configLabel(), false);
                if (row.tooltipKey() != null && y >= viewTop() && y + 20 <= viewBottom()
                    && mouseX >= optLabelX() - 4 && mouseX <= inputX() - 10 && mouseY >= y && mouseY <= y + 20)
                    tooltipKey = row.tooltipKey();
            }
            if (row.extraText() != null && !row.extraText().isEmpty()) {
                g.drawText(textRenderer, Text.literal(truncate(row.extraText(), rightAreaW())),
                    optLabelX(), y + 21, c().textSecondary(), false);
            }

            for (Element w : row.widgets()) {
                if (w instanceof TextFieldWidget eb && boxErrors.containsKey(eb)) {
                    g.drawText(textRenderer, Text.literal(truncate(boxErrors.get(eb), rightAreaW())),
                        optLabelX(), y + 22, 0xFFFF4444, false);
                    break;
                }
            }

            if (genError != null && row == genInputRow) {
                g.drawText(textRenderer, Text.literal(truncate(genError, rightAreaW())),
                    optLabelX(), y + 22, 0xFFFF4444, false);
            }
            y += row.height();
        }
        g.disableScissor();
        drawBar(g, rTrackX(), viewTop(), viewBottom(), rTotalH(), rightPane.offset(), calcMaxScroll(),
            mouseX, mouseY, rightPane.dragging());

        int changed = changeCount();
        doneBtn.visible = changed == 0;
        exitBtn.visible = changed > 0;
        saveBtn.visible = changed > 0;

        super.render(g, mouseX, mouseY, partialTick);

        if (changed > 0)
            g.drawText(textRenderer, Text.translatable("e33chat.config.changed", changed),
                width / 2 + 112, height - 26, c().configLabel(), false);

        if (error != null) {

            g.drawText(textRenderer, Text.literal(truncate(error, rightAreaW())),
                optLabelX(), viewBottom() - 12, 0xFFFF4444, false);
        }

        if (tooltipKey != null)
            g.drawTooltip(textRenderer, Text.translatable(tooltipKey + ".desc"), mouseX, mouseY);
    }

    //#if MC >= 12002
    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {

    }
    //#else
    //$$ @Override
    //$$ public void renderBackground(DrawContext g) {
    //$$     // no-op：背景已在 render() 开头画一次（同客户端）
    //$$ }
    //#endif

    @Override
    public boolean shouldPause() { return true; }
}
