package com.niuqu.chatbubble.store;

import java.util.List;
import net.minecraft.text.Text;

public final class BlockList {
    private BlockList() {}

    public static boolean matchesBlocked(String name, List<? extends String> blocked) {
        if (name == null || name.isEmpty() || blocked == null || blocked.isEmpty()) return false;

        String stripped = name.replaceAll("§.", "").trim();
        for (String b : blocked) {
            if (b == null || b.isBlank()) continue;
            String candidate = b.replaceAll("§.", "").trim();
            if (stripped.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }

    public static boolean isPlayerBlocked(String rawPlayerName, Text senderName, List<? extends String> blocked) {
        if (blocked == null || blocked.isEmpty()) return false;
        if (matchesBlocked(rawPlayerName, blocked)) return true;
        return senderName != null && matchesBlocked(senderName.getString(), blocked);
    }

    public static boolean isBlocked(ChatMessageStore.ChatMessage m, List<? extends String> blocked) {
        return isPlayerBlocked(m.rawPlayerName(), m.senderName(), blocked);
    }

    public static boolean isBlocked(ChatMessageStore.ChatMessage m) {
        return isBlocked(m, com.niuqu.chatbubble.ChatBubbleClientSetup.config().blockedPlayers());
    }
}
