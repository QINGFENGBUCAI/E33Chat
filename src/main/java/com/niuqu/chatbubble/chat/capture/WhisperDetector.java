package com.niuqu.chatbubble.chat.capture;
import com.niuqu.chatbubble.GuiCompat;
import com.niuqu.chatbubble.chat.MessagePresentation;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

// 系统私聊检测
public final class WhisperDetector {
    // 私有构造
    private WhisperDetector() {}

    // 检测系统私聊消息并保留样式
    public static ChatMessageStore.SenderMeta detectWhisperInSystemMessage(Text message, String text, String logTag) {
        var self = MinecraftClient.getInstance().player;
        if (self == null) return null;
        var connection = self.networkHandler;
        if (connection == null) return null;
        String clean = text.replaceAll("§.", "");
        String outgoingPartner = MessagePresentation.outgoingWhisperPartner(clean);
        if (outgoingPartner != null && MessagePresentation.hasWhisperKeywordBeforeColon(clean)) {
            String content = MessagePresentation.extractWhisperContent(clean, outgoingPartner);
            Text ownName = ChatMessageStore.ownDisplayName();
            String ownRaw = self.getName().getString();
            ChatMessageStore.debugLog(() -> "[e33chat] System(" + logTag + "/outgoing) | text='" + clean
                + "' | partner=" + outgoingPartner + " | content='" + content + "'");
            return new ChatMessageStore.SenderMeta(
                self.getUuid(), ownName, styledContent(message, clean, content),
                false, ownRaw, true, outgoingPartner);
        }
        for (var info : connection.getPlayerList()) {
            String profile = GuiCompat.profileName(info.getProfile());
            for (String cand : ChatClassifier.nameCandidates(info)) {
                int idx = clean.indexOf(cand);
                if (idx >= 0 && idx < 30) {
                    if (MessagePresentation.hasWhisperKeywordBeforeColon(clean)) {
                        String content = MessagePresentation.extractWhisperContent(clean, cand);
                        UUID senderId = GuiCompat.profileId(info.getProfile());
                        ChatMessageStore.debugLog(() -> "[e33chat] System(" + logTag + ") | text='" + clean + "' | name=" + cand + " | content='" + content + "'");
                        return new ChatMessageStore.SenderMeta(
                            senderId,
                            styledName(message, clean, idx, cand),
                            styledContent(message, clean, content),
                            false,
                            profile,
                            true, profile
                        );
                    }
                }
            }
        }
        for (var sp : ChatMessageStore.knownNameVariants()) {
            int idx = clean.indexOf(sp);
            if (idx >= 0 && idx < 30) {
                if (MessagePresentation.hasWhisperKeywordBeforeColon(clean)) {
                    UUID su = ChatMessageStore.findSeenUuid(sp);
                    if (su != null) {
                        String content = MessagePresentation.extractWhisperContent(clean, sp);
                        ChatMessageStore.debugLog(() -> "[e33chat] System(" + logTag + "/cache) | text='" + clean + "' | name=" + sp + " | content='" + content + "'");
                        return new ChatMessageStore.SenderMeta(
                            su,
                            styledName(message, clean, idx, sp),
                            styledContent(message, clean, content),
                            false,
                            sp,
                            true, sp
                        );
                    }
                }
            }
        }
        return null;
    }

    // 截取带样式名字
    private static Text styledName(Text message, String clean, int idx, String name) {
        Text decorated = ChatMessageStore.extractWhisperDisplayName(message, Text.literal(name));
        String decoratedStr = decorated.getString();
        if (!decoratedStr.isBlank() && decoratedStr.contains(name)) return decorated;
        Text styled = ChatMessageStore.styledSliceByCleanIndex(message, idx, name.length());
        return styled != null && styled.getString().equals(name) ? styled : Text.literal(name);
    }

    // 截取带样式正文
    private static Text styledContent(Text message, String clean, String content) {
        int idx = clean.indexOf(content);
        if (idx < 0) return Text.literal(content);
        Text styled = ChatMessageStore.styledSliceByCleanIndex(message, idx, content.length());
        return styled != null ? styled : Text.literal(content);
    }
}
