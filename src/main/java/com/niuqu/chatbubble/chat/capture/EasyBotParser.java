package com.niuqu.chatbubble.chat.capture;

import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.Text;

public final class EasyBotParser {
    private EasyBotParser() {}

    private static final Pattern RELAY_FORMAT = Pattern.compile(
        "^(?:\\[([^\\]]*)\\]\\s*)?<([^>]*)>\\s*(?s:(.*))$");

    private static final Pattern RELAY_COLON_FORMAT = Pattern.compile(
        "^\\[([^\\]]*)\\]\\s*([^<>\\[\\]]{1,32}?)\\s*[:：]\\s*(?s:(.*))$");

    private static final Pattern QQ_AT_END = Pattern.compile("[（(]?(\\d{5,12})[)）]?$");

    private static final int MAX_NAME = 32;

    private static final Set<String> BROADCAST_LABELS = Set.of(
        "系统", "公告", "服务器", "广播", "提示", "通知",
        "system", "server", "notice", "broadcast", "announcement", "alert");

    private static boolean isSystemLikeLabel(String label) {
        if (label == null || label.isBlank()) return true;
        String s = label.trim().toLowerCase(java.util.Locale.ROOT);
        for (String token : BROADCAST_LABELS) {
            if (s.contains(token)) return true;
        }
        return s.endsWith("系统") || s.endsWith("插件") || s.endsWith("助手");
    }

    public static ChatMessageStore.SenderMeta tryParse(Text message, String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher angle = RELAY_FORMAT.matcher(text);
        if (angle.matches()) return build(message, angle, true);

        Matcher colon = RELAY_COLON_FORMAT.matcher(text);
        if (colon.matches()) {

            if (isSystemLikeLabel(colon.group(1))) return null;
            String colonContent = colon.group(3);
            if (colonContent != null && colonContent.stripLeading().startsWith("/")) return null;
            return build(message, colon, false);
        }
        return null;
    }

    private static ChatMessageStore.SenderMeta build(Text message, Matcher m, boolean allowStepAside) {
        String groupName = m.group(1) == null ? "" : m.group(1).trim();
        String nameArea = m.group(2) == null ? "" : m.group(2).trim();
        String content = m.group(3);
        if (nameArea.isEmpty() || content == null || content.isBlank()) return null;
        if (nameArea.length() > MAX_NAME || nameArea.indexOf('\n') >= 0) return null;

        String nick = null;
        String qq = null;
        Matcher qm = QQ_AT_END.matcher(nameArea);
        if (qm.find()) {
            qq = qm.group(1);
            String before = nameArea.substring(0, qm.start()).trim();

            int paren = before.lastIndexOf('(');
            if (paren < 0) paren = before.lastIndexOf('（');
            if (paren >= 0) before = before.substring(0, paren).trim();
            if (!before.isEmpty()) nick = before;
        } else if (nameArea.matches("\\d{5,12}")) {
            qq = nameArea;
        } else {
            nick = nameArea;
        }

        String displayName = (nick != null && !nick.isEmpty()) ? nick : qq;
        if (displayName == null || displayName.isEmpty()) return null;

        if (qq == null && (isBroadcastLabel(groupName) || isBroadcastLabel(displayName))) return null;

        if (allowStepAside && isKnownPlayer(displayName)) return null;

        UUID uuid = allowStepAside ? new UUID(0, 0) : resolveUuid(displayName);
        String rawPlayerName = qq != null ? qq : displayName;

        Text contentComp = ChatMessageStore.sliceStyled(message, m.start(3), m.end(3));

        Text nameComp = (uuid != null && !uuid.equals(new UUID(0, 0)))
            ? ChatPipeline.extractDecoratedName(message, content, displayName, Text.literal(displayName))
            : Text.literal(displayName);
        return new ChatMessageStore.SenderMeta(
            uuid, nameComp, contentComp, false,
            rawPlayerName, false, null);
    }

    private static UUID resolveUuid(String name) {
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null && mc.player.networkHandler != null) {
                for (net.minecraft.client.network.PlayerListEntry info : mc.player.networkHandler.getPlayerList()) {
                    for (String cand : ChatClassifier.nameCandidates(info)) {
                        if (cand.equalsIgnoreCase(name)) return info.getProfile().getId();
                    }
                }
            }
        } catch (Throwable t) {

        }
        UUID seen = ChatMessageStore.findSeenUuid(name);
        return seen != null ? seen : new UUID(0, 0);
    }

    private static boolean isKnownPlayer(String displayName) {
        try {
            if (ChatMessageStore.knownNameVariants().contains(displayName)) return true;
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.player.networkHandler == null) return false;
            for (net.minecraft.client.network.PlayerListEntry info : mc.player.networkHandler.getPlayerList()) {
                for (String cand : ChatClassifier.nameCandidates(info)) {
                    if (cand.equalsIgnoreCase(displayName)) return true;
                }
            }
        } catch (Throwable t) {

            return false;
        }
        return false;
    }

    private static boolean isBroadcastLabel(String s) {
        String zone = s.trim();
        while (zone.length() >= 2) {
            char open = zone.charAt(0);
            char close = zone.charAt(zone.length() - 1);
            if ((open == '[' && close == ']') || (open == '【' && close == '】')
                || (open == '<' && close == '>') || (open == '(' && close == ')')
                || (open == '（' && close == '）')) {
                zone = zone.substring(1, zone.length() - 1).trim();
            } else {
                break;
            }
        }
        return !zone.isEmpty() && BROADCAST_LABELS.contains(zone.toLowerCase(java.util.Locale.ROOT));
    }
}
