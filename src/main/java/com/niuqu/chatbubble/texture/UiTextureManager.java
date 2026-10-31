package com.niuqu.chatbubble.texture;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import net.minecraft.util.Identifier;

// UI纹理地址管理
public final class UiTextureManager {
    // 禁止实例化
    private UiTextureManager() {}

    // 取当前主题纹理
    public static Identifier rl(UiElement el) {
        return el.rl(currentTheme());
    }

    // 取指定主题纹理
    public static Identifier rl(UiElement el, ChatBubbleTheme theme) {
        return el.rl(theme);
    }

    // 取当前主题
    public static ChatBubbleTheme currentTheme() {
        return ChatBubbleClientSetup.config().theme().equalsIgnoreCase("light")
            ? ChatBubbleTheme.LIGHT : ChatBubbleTheme.DARK;
    }
}
