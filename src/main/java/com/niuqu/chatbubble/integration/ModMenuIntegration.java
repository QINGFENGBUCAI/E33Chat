package com.niuqu.chatbubble.integration;
import com.niuqu.chatbubble.config.ChatBubbleConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// 模组菜单集成入口
public class ModMenuIntegration implements ModMenuApi {
    // 提供配置界面工厂
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ChatBubbleConfigScreen::new;
    }
}
