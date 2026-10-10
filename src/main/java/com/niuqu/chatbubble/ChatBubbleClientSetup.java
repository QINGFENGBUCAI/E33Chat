package com.niuqu.chatbubble;
import com.niuqu.chatbubble.config.ChatBubbleConfigScreen;
import com.niuqu.chatbubble.render.ChatBubbleHudOverlay;
import com.niuqu.chatbubble.render.RoundRectRenderer;

import com.niuqu.chatbubble.config.ChatBubbleConfig;
import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.config.ServerConfigScreen;
import com.niuqu.chatbubble.config.ConfigManager;
import com.niuqu.chatbubble.image.ImageLoader;
import com.niuqu.chatbubble.network.ChatMetaPayload;
import com.niuqu.chatbubble.network.ConfigSyncPayload;
import com.niuqu.chatbubble.network.ConfigSyncV2Payload;
import com.niuqu.chatbubble.network.EasyBotConfigPayload;
import com.niuqu.chatbubble.network.HistoryPayload;
import com.niuqu.chatbubble.network.MediaCapPayload;
import com.niuqu.chatbubble.network.ServerConfigScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#if MC >= 26000
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//#endif
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

public class ChatBubbleClientSetup implements ClientModInitializer {
    private static ChatBubbleConfig config = ChatBubbleConfig.defaults();
    private static Path configPath;
    private static boolean leftWasDown;

    public static ChatBubbleConfig config() { return config; }

    public static void saveConfig(ChatBubbleConfig newConfig) {
        config = newConfig;
        E33Log.info("[e33chat] Saving config | soundPublic=" + newConfig.soundPublic() + " | soundSystem=" + newConfig.soundSystem());
        ConfigManager.save(configPath, config);
    }

    @Override
    public void onInitializeClient() {
        Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/e33chat");
        configPath = configDir.resolve("e33chat-client.json");

        Path recentDirPath = configDir.resolve("client.json");
        Path legacyPath = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/e33chat-client.json");
        Path oldFlatPath = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/e33chat.json");
        if (!Files.exists(configPath)) {
            if (Files.exists(recentDirPath)) {
                try {
                    Files.move(recentDirPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    E33Log.info("[e33chat] Migrated config from config/e33chat/client.json to config/e33chat/e33chat-client.json");
                } catch (Exception e) {
                    E33Log.warn("[e33chat] Config migration failed", e);
                }
            } else if (Files.exists(legacyPath)) {
                try {
                    Files.createDirectories(configDir);
                    Files.move(legacyPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    E33Log.info("[e33chat] Migrated config from config/e33chat-client.json to config/e33chat/e33chat-client.json");
                } catch (Exception e) {
                    E33Log.warn("[e33chat] Config migration failed", e);
                }
            } else if (Files.exists(oldFlatPath)) {
                config = ConfigManager.load(oldFlatPath);
                ConfigManager.save(configPath, config);
                try {
                    Files.delete(oldFlatPath);
                } catch (Exception e) {
                    E33Log.warn("[e33chat] Legacy config cleanup failed", e);
                }
                E33Log.info("[e33chat] Migrated config from config/e33chat.json to config/e33chat/e33chat-client.json");
            }
        }

        if (Files.exists(configPath)) {
            config = ConfigManager.load(configPath);
        } else if (Files.exists(recentDirPath)) {
            config = ConfigManager.load(recentDirPath);
        } else if (Files.exists(legacyPath)) {
            config = ConfigManager.load(legacyPath);
        } else if (Files.exists(oldFlatPath)) {
            config = ConfigManager.load(oldFlatPath);
        } else {
            config = ConfigManager.load(configPath);
        }

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(ChatMetaPayload.ID, (payload, context) -> {
            context.client().execute(() -> ChatMessageStore.applyChatMeta(
                payload.senderUUID(), payload.senderName(), payload.messageHash(),
                payload.quoteSender(), payload.quoteContent(), payload.mentionTargets()));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(ChatMetaPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     ChatMetaPayload payload = ChatMetaPayload.read(buf);
        //$$     client.execute(() -> ChatMessageStore.applyChatMeta(
        //$$         payload.senderUUID(), payload.senderName(), payload.messageHash(),
        //$$         payload.quoteSender(), payload.quoteContent(), payload.mentionTargets()));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(HistoryPayload.ID, (payload, context) -> {
            context.client().execute(() -> ChatMessageStore.addHistoryMessages(payload.entries()));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(HistoryPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     HistoryPayload payload = HistoryPayload.read(buf);
        //$$     client.execute(() -> ChatMessageStore.addHistoryMessages(payload.entries()));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> ConfigSyncPayload.handle(payload));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     ConfigSyncPayload payload = ConfigSyncPayload.read(buf);
        //$$     client.execute(() -> ConfigSyncPayload.handle(payload));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncV2Payload.ID, (payload, context) -> {
            context.client().execute(() -> ConfigSyncV2Payload.handle(payload));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(ConfigSyncV2Payload.ID, (client, handler, buf, responseSender) -> {
        //$$     ConfigSyncV2Payload payload = ConfigSyncV2Payload.read(buf);
        //$$     client.execute(() -> ConfigSyncV2Payload.handle(payload));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(EasyBotConfigPayload.ID, (payload, context) -> {
            context.client().execute(() -> EasyBotConfigPayload.handle(payload));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(EasyBotConfigPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     EasyBotConfigPayload payload = EasyBotConfigPayload.read(buf);
        //$$     client.execute(() -> EasyBotConfigPayload.handle(payload));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(ServerConfigScreenPayload.ID, (payload, context) -> {
            context.client().execute(() -> MinecraftClient.getInstance().setScreen(new ServerConfigScreen(
                MinecraftClient.getInstance().currentScreen,
                payload.useTpa(), payload.historyEnabled(), payload.templateDebug(),
                payload.mediaEnabled(), payload.mediaAutoClean(), payload.easyBotCompat(),
                payload.groupsEnabled(),
                payload.chatTemplates(), payload.whisperTemplates())));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(ServerConfigScreenPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     ServerConfigScreenPayload payload = ServerConfigScreenPayload.read(buf);
        //$$     client.execute(() -> MinecraftClient.getInstance().setScreen(new ServerConfigScreen(
        //$$         MinecraftClient.getInstance().currentScreen,
        //$$         payload.useTpa(), payload.historyEnabled(), payload.templateDebug(),
        //$$         payload.mediaEnabled(), payload.mediaAutoClean(), payload.easyBotCompat(),
        //$$         payload.groupsEnabled(),
        //$$         payload.chatTemplates(), payload.whisperTemplates())));
        //$$ });
        //#endif

        com.niuqu.chatbubble.image.MediaClient.registerReceivers();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            com.niuqu.chatbubble.chat.GroupChannelState.reset();
            com.niuqu.chatbubble.render.BlurRenderer.setDisconnecting(false);
            com.niuqu.chatbubble.network.ClientHelloPayload.send();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            com.niuqu.chatbubble.chat.GroupChannelState.reset();

            com.niuqu.chatbubble.render.BlurRenderer.setDisconnecting(true);
            com.niuqu.chatbubble.render.BlurRenderer.cleanup();
        });

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(com.niuqu.chatbubble.network.GroupChatPayload.ID, (payload, context) -> {
            context.client().execute(() -> com.niuqu.chatbubble.network.GroupChatPayload.handleClient(payload));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(com.niuqu.chatbubble.network.GroupChatPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     com.niuqu.chatbubble.network.GroupChatPayload payload = com.niuqu.chatbubble.network.GroupChatPayload.read(buf);
        //$$     client.execute(() -> com.niuqu.chatbubble.network.GroupChatPayload.handleClient(payload));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(com.niuqu.chatbubble.network.GroupListPayload.ID, (payload, context) -> {
            context.client().execute(() -> com.niuqu.chatbubble.network.GroupListPayload.handleClient(payload));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(com.niuqu.chatbubble.network.GroupListPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     com.niuqu.chatbubble.network.GroupListPayload payload = com.niuqu.chatbubble.network.GroupListPayload.read(buf);
        //$$     client.execute(() -> com.niuqu.chatbubble.network.GroupListPayload.handleClient(payload));
        //$$ });
        //#endif

        //#if MC >= 12005
        ClientPlayNetworking.registerGlobalReceiver(MediaCapPayload.ID, (payload, context) -> {
            context.client().execute(() -> MediaCapPayload.handle(payload));
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(MediaCapPayload.ID, (client, handler, buf, responseSender) -> {
        //$$     MediaCapPayload payload = MediaCapPayload.read(buf);
        //$$     client.execute(() -> MediaCapPayload.handle(payload));
        //$$ });
        //#endif

        ChatMessageStore.setMessageEffectObserver(
            new com.niuqu.chatbubble.chat.notification.ChatMessageEffects());

        //#if MC >= 26000

        HudElementRegistry.addLast(Identifier.of("e33chat", "bubble_overlay"), (drawContext, tickDelta) -> {
            if (!config.enabled()) return;
            ChatBubbleHudOverlay.render(drawContext);
        });
        //#else
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!config.enabled()) return;
            //#if MC >= 12000
            ChatBubbleHudOverlay.render(drawContext);
            //#else

            //$$ ChatBubbleHudOverlay.render(new com.niuqu.chatbubble.DrawContext((net.minecraft.client.util.math.MatrixStack) drawContext));
            //#endif
        });
        //#endif

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ImageLoader.tick();
            com.niuqu.chatbubble.image.AnimatedImageLoader.tick();

            if (!config.enabled()) return;

            String key;
            if (client.world == null || client.player == null) {
                key = null;
            } else if (client.getServer() != null) {
                key = "SP:" + client.getServer().getSaveProperties().getLevelName();
            } else if (client.getCurrentServerEntry() != null) {
                key = "MP:" + client.getCurrentServerEntry().name;
            } else {
                key = "world";
            }
            ChatMessageStore.setCurrentWorld(key);
            ChatMessageStore.maybeAutoSave();

            if (client.currentScreen == null) {
                //#if MC >= 26000

                boolean leftDown = client.mouseHandler.isLeftPressed();
                //#else
                //$$ boolean leftDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                //$$     client.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                //#endif
                if (leftDown && !leftWasDown) {
                    double mx = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
                    double my = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
                    if (ChatBubbleHudOverlay.isMouseOverIcon(mx, my)) {
                        client.setScreen(new ChatBubbleScreen(""));
                    }
                }
                leftWasDown = leftDown;
            } else {
                leftWasDown = false;
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) ->
            ScreenEvents.afterRender(screen).register((scr, g, mouseX, mouseY, delta) -> {
                if (config.enabled()) {
                    //#if MC >= 12000
                    ChatBubbleHudOverlay.renderBannerForScreen(g);
                    //#else

                    //$$ ChatBubbleHudOverlay.renderBannerForScreen(new com.niuqu.chatbubble.DrawContext((net.minecraft.client.util.math.MatrixStack) g));
                    //#endif
                }
            })
        );

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.of(ChatBubbleMod.MOD_ID, "shader_reload");
                }
                @Override
                public void reload(ResourceManager manager) {
                    RoundRectRenderer.resetShader();
                }
            }
        );
    }
}
