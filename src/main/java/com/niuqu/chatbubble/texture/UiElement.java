package com.niuqu.chatbubble.texture;

import com.niuqu.chatbubble.render.ChatBubbleTheme;
import net.minecraft.util.Identifier;

public enum UiElement {
    PANEL_BG("panel_bg"),
    TITLE_BAR("title_bar"),
    BOTTOM_BAR("bottom_bar"),
    SIDEBAR_BG("sidebar_bg"),
    DIVIDER("divider"),
    INPUT_BG("input_bg"),
    SCROLLBAR_TRACK("scrollbar_track"),
    SCROLLBAR_THUMB("scrollbar_thumb"),
    CONTEXT_MENU_BG("context_menu_bg"),
    POPUP_BG("popup_bg"),
    TOAST_BG("toast_bg"),
    WHISPER_BAR("whisper_bar"),
    CONFIG_BG("config_bg"),
    CONTENT_BG("content_bg"),

    QUICK_SCROLLBAR_TRACK("quick_scrollbar_track"),
    QUICK_SCROLLBAR_THUMB("quick_scrollbar_thumb"),

    HOVER_BG("hover_bg"),
    SIDEBAR_SELECTED("sidebar_selected"),
    SIDEBAR_HOVER("sidebar_hover"),
    CONTEXT_HOVER("context_hover"),
    CLOSE_BG("close_bg"),
    CLOSE_HOVER("close_hover");

    private final String path;

    UiElement(String path) {
        this.path = path;
    }

    public Identifier rl(ChatBubbleTheme theme) {
        return Identifier.of("e33chat",
            "textures/gui/" + theme.name().toLowerCase() + "/" + path + ".png");
    }
}
