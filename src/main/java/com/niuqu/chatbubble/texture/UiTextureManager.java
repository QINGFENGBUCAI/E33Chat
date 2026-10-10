package com.niuqu.chatbubble.texture;

import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import net.minecraft.util.Identifier;

public final class UiTextureManager {

    private UiTextureManager() {}

    public static Identifier rl(UiElement el) {
        return el.rl(currentTheme());
    }

    public static Identifier rl(UiElement el, ChatBubbleTheme theme) {
        return el.rl(theme);
    }

    public static ChatBubbleTheme currentTheme() {
        return ChatBubbleClientSetup.config().theme().equalsIgnoreCase("light")
            ? ChatBubbleTheme.LIGHT : ChatBubbleTheme.DARK;
    }
}
