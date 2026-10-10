package com.niuqu.chatbubble.chat.capture;

import com.niuqu.chatbubble.GuiCompat;
import com.niuqu.chatbubble.chat.TemplateMatcher;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class TemplateLayer {
    private TemplateLayer() {}

    private static long templateMissWindowStart;
    private static int templateMissBurst;

    public static void logTemplateMiss(String text) {
        logTemplateMiss(text, "System");
    }

    public static void logTemplateMiss(String text, String logTag) {
        if (!ChatMessageStore.serverTemplateDebug()) return;
        long now = System.currentTimeMillis();
        if (now - templateMissWindowStart >= 60_000) {
            templateMissWindowStart = now;
            templateMissBurst = 0;
        }
        if (++templateMissBurst > 5) return;
        String s = text.length() <= 100 ? text : text.substring(0, 100) + "…";

        StringBuilder tpl = new StringBuilder();
        for (var t : ChatMessageStore.serverChatTemplates()) tpl.append("\n  chat: ").append(t.raw());
        for (var t : ChatMessageStore.serverWhisperTemplates()) tpl.append("\n  whisper: ").append(t.raw());
        ChatMessageStore.debugLog(() -> "[e33chat] " + logTag + "(template miss) | text='" + s + "' | templates=" + tpl);
    }

    public static boolean isTemplateNameKnown(String name) {
        if (name == null || name.isEmpty()) return false;
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            String myName = player.getName().getString();
            if (!myName.isEmpty() && (name.equals(myName) || name.contains(myName))) return true;
        }
        return ChatClassifier.resolveOnlinePlayer(name) != null || ChatMessageStore.findSeenUuid(name) != null;
    }

    public static ChatMessageStore.SenderMeta matchByTemplate(Text message, String text) {
        return matchByTemplate(message, text, "System");
    }

    public static ChatMessageStore.SenderMeta matchByTemplate(Text message, String text, String logTag) {
        var r = TemplateMatcher.match(text, ChatMessageStore.serverChatTemplates(),
            ChatMessageStore.serverWhisperTemplates(), TemplateLayer::isTemplateNameKnown);
        if (r.isEmpty()) {
            logTemplateMiss(text, logTag);
            return null;
        }
        var tpl = r.orElseThrow();
        String verified = tpl.verifiedName();
        var info = ChatClassifier.resolveOnlinePlayer(verified);
        UUID uid = info != null ? GuiCompat.profileId(info.getProfile()) : ChatMessageStore.findSeenUuid(verified);
        String rawName = info != null ? GuiCompat.profileName(info.getProfile()) : verified;
        boolean isSelf = uid != null && MinecraftClient.getInstance().player != null
            && uid.equals(MinecraftClient.getInstance().player.getUuid());
        if (isSelf) {
            if (tpl.whisper()) {

                ChatMessageStore.markSuppressCapture();
                ChatMessageStore.debugLog(() -> "[e33chat] " + logTag + "(template outgoing whisper) | text='" + text + "'");
                return null;
            }

            ChatMessageStore.cacheOwnDecoratedName(
                templateSlice(message, text, tpl.nameStart(), tpl.nameEnd()));
            ChatMessageStore.debugLog(() -> "[e33chat] " + logTag + "(template own line) | text='" + text + "'");
            return null;
        }
        Text nameComp = templateSlice(message, text, tpl.nameStart(), tpl.nameEnd());
        Text contentComp = templateSlice(message, text, tpl.contentStart(), tpl.contentEnd());
        boolean whisper = tpl.whisper();
        String partner = whisper ? tpl.sender() : null;
        ChatMessageStore.debugLog(() -> "[e33chat] " + logTag + "(template) | text='" + text + "' | name='" + nameComp.getString() + "' | whisper=" + whisper + " | partner=" + partner + " | content='" + contentComp.getString() + "'");
        return new ChatMessageStore.SenderMeta(uid != null ? uid : new UUID(0, 0), nameComp, contentComp,
            false, rawName, whisper, partner);
    }

    public static Text templateSlice(Text message, String text, int from, int to) {
        String sub = text.substring(from, to);
        if (sub.indexOf('§') >= 0) return ChatMessageStore.parseStyledText(sub);
        return ChatMessageStore.sliceStyled(message, from, to);
    }
}
