package com.niuqu.chatbubble.config;
import com.google.gson.annotations.SerializedName;
import java.util.List;

// 聊天气泡客户端配置
public record ChatBubbleConfig(
    boolean enabled,
    String theme,
    boolean redDotEnabled,
    boolean hideChatIcon,
    @SerializedName("hud_icon_x") Integer hudIconX,
    @SerializedName("hud_icon_y") Integer hudIconY,
    boolean animationEnabled,
    boolean systemChatAsBubble,
    boolean antiSpam,
    boolean chatHistoryEnabled,
    int historyRetentionDays,
    int timeSeparatorMinutes,
    int panelWidth,
    boolean panelFullscreen,
    int bubbleCornerRadius,
    String ownBubbleColor,
    String otherBubbleColor,
    String ownTextColor,
    String otherTextColor,
    boolean soundPublic,
    boolean soundSystem,
    boolean soundWhisper,
    boolean debugLog,
    boolean preserveInput,
    boolean colorCodes,
    List<String> sidebarHidePatterns,
    List<String> blockedPlayers,
    List<String> quickChatPhrases,
    boolean mentionBannerEnabled,
    boolean systemBannerEnabled,
    Integer mentionBannerDuration,
    boolean mentionSoundEnabled,
    boolean mentionRequireAt,
    boolean mentionWhisperBanner,
    boolean blurEnabled,
    Integer panelOpacity,
    Integer soundVolume,
    boolean ownMentionNotify,
    boolean ownQuoteNotify,
    boolean ownWhisperNotify,
    Integer bannerCornerRadius,
    @SerializedName("banner_offset_x") int bannerOffsetX,
    @SerializedName("banner_offset_y") int bannerOffsetY,
    @SerializedName("banner_max_stack") Integer bannerMaxStack,
    String panelAnimStyle,
    String bannerAnimStyle,
    String popupAnimStyle,
    String messageAnimStyle,
    Boolean imageRenderEnabled,
    Boolean receiveImages,
    String uploadUrl,
    String uploadField,
    String uploadExtra,
    String uploadResponse,
    Integer messageGap,
    Integer avatarSize,
    Boolean hideRepeatedAvatars,
    boolean closeChatOnSend,
    Integer bannerOpacity,
    Integer bubbleSize,
    @SerializedName("panel_bg_image") String panelBgImage,
    @SerializedName("panel_bg_opacity") Integer panelBgOpacity,
    @SerializedName("panel_bg_crop") String panelBgCrop
) {
    // 生成默认配置
    public static ChatBubbleConfig defaults() {
        return new ChatBubbleConfig(
            true, "dark", true, false, 3, 20, true,
            false, true,
            false, 0, 5, 1000, false, 4,
            "#1E90FF", "#4A4A4A", "#FFFFFF", "#FFFFFF",
            false, false, true, false, true, false,
            List.of(), List.of(), List.of(),
            true, true, 4, true, true, true,
            false, 80, 80, false, false, false, 4, 0, 0, 3,
            "slide", "slide", "fade", "fade",
            true, true,
            null, null, null, null,
            6, 20, false, false, 100, 9,
            "", 100, null
        );
    }

    // 解析十六进制颜色
    public static int parseHexColor(String hex, int defaultColor) {
        if (hex == null) return defaultColor;
        try {
            String h = hex.replace("#", "").trim();
            if (h.length() != 6) return defaultColor;
            return 0xFF000000 | Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }

    // 替换主题
    public ChatBubbleConfig withTheme(String theme) {
        return new ChatBubbleConfig(enabled, theme, redDotEnabled, hideChatIcon, hudIconX, hudIconY, animationEnabled,
            systemChatAsBubble, antiSpam,
            chatHistoryEnabled, historyRetentionDays, timeSeparatorMinutes,
            panelWidth, panelFullscreen, bubbleCornerRadius, ownBubbleColor, otherBubbleColor, ownTextColor, otherTextColor,
            soundPublic, soundSystem, soundWhisper, debugLog, preserveInput, colorCodes, sidebarHidePatterns, blockedPlayers, quickChatPhrases,
            mentionBannerEnabled, systemBannerEnabled, mentionBannerDuration, mentionSoundEnabled, mentionRequireAt, mentionWhisperBanner,
            blurEnabled, panelOpacity, soundVolume, ownMentionNotify, ownQuoteNotify, ownWhisperNotify, bannerCornerRadius, bannerOffsetX, bannerOffsetY, bannerMaxStack,
            panelAnimStyle, bannerAnimStyle, popupAnimStyle, messageAnimStyle, imageRenderEnabled, receiveImages,
            uploadUrl, uploadField, uploadExtra, uploadResponse, messageGap, avatarSize, hideRepeatedAvatars, closeChatOnSend, bannerOpacity, bubbleSize,
            "", 100, null);
    }

    // 替换快捷短语
    public ChatBubbleConfig withQuickChatPhrases(List<String> phrases) {
        return new ChatBubbleConfig(enabled, theme, redDotEnabled, hideChatIcon, hudIconX, hudIconY, animationEnabled,
            systemChatAsBubble, antiSpam,
            chatHistoryEnabled, historyRetentionDays, timeSeparatorMinutes,
            panelWidth, panelFullscreen, bubbleCornerRadius, ownBubbleColor, otherBubbleColor, ownTextColor, otherTextColor,
            soundPublic, soundSystem, soundWhisper, debugLog, preserveInput, colorCodes, sidebarHidePatterns, blockedPlayers, phrases,
            mentionBannerEnabled, systemBannerEnabled, mentionBannerDuration, mentionSoundEnabled, mentionRequireAt, mentionWhisperBanner,
            blurEnabled, panelOpacity, soundVolume, ownMentionNotify, ownQuoteNotify, ownWhisperNotify, bannerCornerRadius, bannerOffsetX, bannerOffsetY, bannerMaxStack,
            panelAnimStyle, bannerAnimStyle, popupAnimStyle, messageAnimStyle, imageRenderEnabled, receiveImages,
            uploadUrl, uploadField, uploadExtra, uploadResponse, messageGap, avatarSize, hideRepeatedAvatars, closeChatOnSend, bannerOpacity, bubbleSize, panelBgImage, panelBgOpacity, panelBgCrop);
    }

    // 替换屏蔽玩家
    public ChatBubbleConfig withBlockedPlayers(List<String> blocked) {
        return new ChatBubbleConfig(enabled, theme, redDotEnabled, hideChatIcon, hudIconX, hudIconY, animationEnabled,
            systemChatAsBubble, antiSpam,
            chatHistoryEnabled, historyRetentionDays, timeSeparatorMinutes,
            panelWidth, panelFullscreen, bubbleCornerRadius, ownBubbleColor, otherBubbleColor, ownTextColor, otherTextColor,
            soundPublic, soundSystem, soundWhisper, debugLog, preserveInput, colorCodes, sidebarHidePatterns, blocked, quickChatPhrases,
            mentionBannerEnabled, systemBannerEnabled, mentionBannerDuration, mentionSoundEnabled, mentionRequireAt, mentionWhisperBanner,
            blurEnabled, panelOpacity, soundVolume, ownMentionNotify, ownQuoteNotify, ownWhisperNotify, bannerCornerRadius, bannerOffsetX, bannerOffsetY, bannerMaxStack,
            panelAnimStyle, bannerAnimStyle, popupAnimStyle, messageAnimStyle, imageRenderEnabled, receiveImages,
            uploadUrl, uploadField, uploadExtra, uploadResponse, messageGap, avatarSize, hideRepeatedAvatars, closeChatOnSend, bannerOpacity, bubbleSize, panelBgImage, panelBgOpacity, panelBgCrop);
    }

    // 替换面板背景
    public ChatBubbleConfig withPanelBg(String image, Integer opacity, String crop) {
        return new ChatBubbleConfig(enabled, theme, redDotEnabled, hideChatIcon, hudIconX, hudIconY, animationEnabled,
            systemChatAsBubble, antiSpam,
            chatHistoryEnabled, historyRetentionDays, timeSeparatorMinutes,
            panelWidth, panelFullscreen, bubbleCornerRadius, ownBubbleColor, otherBubbleColor, ownTextColor, otherTextColor,
            soundPublic, soundSystem, soundWhisper, debugLog, preserveInput, colorCodes, sidebarHidePatterns, blockedPlayers, quickChatPhrases,
            mentionBannerEnabled, systemBannerEnabled, mentionBannerDuration, mentionSoundEnabled, mentionRequireAt, mentionWhisperBanner,
            blurEnabled, panelOpacity, soundVolume, ownMentionNotify, ownQuoteNotify, ownWhisperNotify, bannerCornerRadius, bannerOffsetX, bannerOffsetY, bannerMaxStack,
            panelAnimStyle, bannerAnimStyle, popupAnimStyle, messageAnimStyle, imageRenderEnabled, receiveImages,
            uploadUrl, uploadField, uploadExtra, uploadResponse, messageGap, avatarSize, hideRepeatedAvatars, closeChatOnSend, bannerOpacity, bubbleSize,
            image, opacity, crop);
    }

    // 替换背景裁剪
    public ChatBubbleConfig withPanelBgCrop(String crop) {
        return withPanelBg(panelBgImage, panelBgOpacity, crop);
    }

    // 判断玩家是否隐藏
    public boolean isSidebarHidden(String playerName) {
        if (sidebarHidePatterns == null || sidebarHidePatterns.isEmpty()) return false;
        String lowerName = playerName.toLowerCase();
        for (String pattern : sidebarHidePatterns) {
            if (pattern == null || pattern.isEmpty()) continue;
            String regex = "^" + pattern.toLowerCase()
                .replace("*", ".*")
                .replace("?", ".") + "$";
            if (lowerName.matches(regex)) return true;
        }
        return false;
    }
}
