package com.niuqu.chatbubble.config;

import com.niuqu.chatbubble.E33Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private ConfigManager() {}

    public static ChatBubbleConfig load(Path path) {
        if (Files.exists(path)) {
            try (Reader r = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                ChatBubbleConfig loaded = GSON.fromJson(r, ChatBubbleConfig.class);
                if (loaded != null) {
                    var merged = mergeWithDefaults(loaded);
                    E33Log.info("[e33chat] Loaded config | soundPublic=" + merged.soundPublic() + " | soundSystem=" + merged.soundSystem());
                    return merged;
                }
            } catch (Exception e) {
                E33Log.warn("[e33chat] Failed to load config, using defaults", e);

                try {
                    Files.move(path, path.resolveSibling(path.getFileName() + ".bak"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}
            }
        }
        ChatBubbleConfig def = ChatBubbleConfig.defaults();
        save(path, def);
        E33Log.info("[e33chat] Created default config | soundPublic=" + def.soundPublic() + " | soundSystem=" + def.soundSystem());
        return def;
    }

    private static ChatBubbleConfig mergeWithDefaults(ChatBubbleConfig c) {
        ChatBubbleConfig d = ChatBubbleConfig.defaults();
        return new ChatBubbleConfig(
            c.enabled(), c.theme() != null ? c.theme() : d.theme(),
            c.redDotEnabled(), c.hideChatIcon(),
            c.hudIconX() != null ? c.hudIconX() : d.hudIconX(),
            c.hudIconY() != null ? c.hudIconY() : d.hudIconY(),
            c.animationEnabled(),
            c.systemChatAsBubble(),
            c.antiSpam(), c.chatHistoryEnabled(),
            c.historyRetentionDays(), c.timeSeparatorMinutes(),
            c.panelWidth(), c.panelFullscreen(), c.bubbleCornerRadius(),
            c.ownBubbleColor() != null ? c.ownBubbleColor() : d.ownBubbleColor(),
            c.otherBubbleColor() != null ? c.otherBubbleColor() : d.otherBubbleColor(),
            c.ownTextColor() != null ? c.ownTextColor() : d.ownTextColor(),
            c.otherTextColor() != null ? c.otherTextColor() : d.otherTextColor(),
            c.soundPublic(), c.soundSystem(), c.soundWhisper(),
            c.debugLog(), c.preserveInput(), c.colorCodes(),
            c.sidebarHidePatterns() != null ? c.sidebarHidePatterns() : d.sidebarHidePatterns(),
            c.blockedPlayers() != null ? c.blockedPlayers() : d.blockedPlayers(),
            c.quickChatPhrases() != null ? c.quickChatPhrases() : d.quickChatPhrases(),
            c.mentionBannerEnabled(),
            c.systemBannerEnabled(),
            c.mentionBannerDuration() != null ? c.mentionBannerDuration() : d.mentionBannerDuration(),
            c.mentionSoundEnabled(),
            c.mentionRequireAt(),
            c.mentionWhisperBanner(),
            c.blurEnabled(),
            c.panelOpacity() != null ? c.panelOpacity() : d.panelOpacity(),
            c.soundVolume() != null ? c.soundVolume() : d.soundVolume(),
            c.ownMentionNotify(), c.ownQuoteNotify(), c.ownWhisperNotify(),
            c.bannerCornerRadius() != null ? c.bannerCornerRadius() : d.bannerCornerRadius(),
            c.bannerOffsetX(), c.bannerOffsetY(),
            c.bannerMaxStack() != null ? c.bannerMaxStack() : d.bannerMaxStack(),
            c.panelAnimStyle() != null ? c.panelAnimStyle() : d.panelAnimStyle(),
            c.bannerAnimStyle() != null ? c.bannerAnimStyle() : d.bannerAnimStyle(),
            c.popupAnimStyle() != null ? c.popupAnimStyle() : d.popupAnimStyle(),
            c.messageAnimStyle() != null ? c.messageAnimStyle() : d.messageAnimStyle(),
            c.imageRenderEnabled() != null ? c.imageRenderEnabled() : d.imageRenderEnabled(),

            c.receiveImages() != null ? c.receiveImages()
                : (c.imageRenderEnabled() != null ? c.imageRenderEnabled() : d.receiveImages()),
            c.uploadUrl() != null ? c.uploadUrl() : d.uploadUrl(),
            c.uploadField() != null ? c.uploadField() : d.uploadField(),
            c.uploadExtra() != null ? c.uploadExtra() : d.uploadExtra(),
            c.uploadResponse() != null ? c.uploadResponse() : d.uploadResponse(),
            c.messageGap() != null ? c.messageGap() : d.messageGap(),
            c.avatarSize() != null ? c.avatarSize() : d.avatarSize(),
            c.hideRepeatedAvatars() != null ? c.hideRepeatedAvatars() : d.hideRepeatedAvatars(),
            c.closeChatOnSend(),
            c.bannerOpacity() != null ? c.bannerOpacity() : d.bannerOpacity(),
            c.bubbleSize() != null ? c.bubbleSize() : d.bubbleSize(),
            c.panelBgImage() != null ? c.panelBgImage() : d.panelBgImage(),
            c.panelBgOpacity() != null ? c.panelBgOpacity() : d.panelBgOpacity(),
            c.panelBgCrop() != null ? c.panelBgCrop() : d.panelBgCrop());
    }

    public static void save(Path path, ChatBubbleConfig config) {
        try {
            Files.createDirectories(path.getParent());

            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(tmp), StandardCharsets.UTF_8)) {
                GSON.toJson(config, w);
            }
            try {
                Files.move(tmp, path,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            E33Log.warn("[e33chat] Failed to save config", e);
        }
    }
}
