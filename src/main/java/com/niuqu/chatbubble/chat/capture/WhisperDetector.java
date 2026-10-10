package com.niuqu.chatbubble.chat.capture;

import com.niuqu.chatbubble.GuiCompat;
import com.niuqu.chatbubble.chat.MessagePresentation;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class WhisperDetector {
    private WhisperDetector() {}

    public static ChatMessageStore.SenderMeta detectWhisperInSystemMessage(String text, String logTag) {
        var self = MinecraftClient.getInstance().player;
        if (self == null) return null;
        var connection = self.networkHandler;
        if (connection == null) return null;

        String clean = text.replaceAll("§.", "");
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
                            Text.literal(cand),
                            Text.literal(content),
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
                            Text.literal(sp),
                            Text.literal(content),
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
}
