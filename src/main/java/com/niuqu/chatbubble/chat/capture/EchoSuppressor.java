package com.niuqu.chatbubble.chat.capture;

import com.niuqu.chatbubble.chat.WhisperSignal;
import com.niuqu.chatbubble.store.ChatMessageStore;
import net.minecraft.client.MinecraftClient;

public final class EchoSuppressor {
    private EchoSuppressor() {}

    public static boolean trySuppressOutgoingEcho(String sysText) {
        boolean hasEchoFlag = ChatMessageStore.hasPendingWhisperEcho();
        boolean hasKw = WhisperSignal.containsZh(sysText)
            || WhisperSignal.EN.matcher(sysText.toLowerCase()).find();
        ChatMessageStore.debugLog(() -> "[e33chat] System(echo check) | text='" + sysText + "' | flag=" + hasEchoFlag + " | kw=" + hasKw);
        if (hasEchoFlag && hasKw) {
            var player = MinecraftClient.getInstance().player;
            boolean otherPlayerFound = false;
            if (player != null && player.networkHandler != null) {
                String myName = player.getName().getString();
                String skipTarget = ChatMessageStore.getPendingWhisperTarget();
                for (var info : player.networkHandler.getPlayerList()) {
                    for (String cand : ChatClassifier.nameCandidates(info)) {
                        if (cand.equals(myName) || cand.isEmpty()) continue;
                        if (cand.equals(skipTarget)) continue;
                        int idx = sysText.indexOf(cand);
                        if (idx >= 0 && idx < 30) {
                            otherPlayerFound = true;
                            break;
                        }
                    }
                    if (otherPlayerFound) break;
                }
            }
            if (!otherPlayerFound) {
                ChatMessageStore.consumeWhisperEcho();
                ChatMessageStore.markSuppressCapture();
                ChatMessageStore.debugLog(() -> "[e33chat] System(echo suppressed) | text='" + sysText + "'");
                return true;
            }
        }
        return false;
    }
}
