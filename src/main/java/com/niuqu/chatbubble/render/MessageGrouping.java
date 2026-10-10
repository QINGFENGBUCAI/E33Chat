package com.niuqu.chatbubble.render;

import com.niuqu.chatbubble.store.ChatMessageStore;

public final class MessageGrouping {
    private MessageGrouping() {}

    public static final long GROUP_TIME_MS = 5 * 60_000L;

    public static int groupedGap(int messageGap) {
        return Math.max(2, messageGap / 3);
    }

    public static boolean isSameGroup(ChatMessageStore.ChatMessage prev, ChatMessageStore.ChatMessage msg) {
        if (prev == null || msg == null) return false;
        if (prev.isSystem() || msg.isSystem()) return false;
        String a = prev.rawPlayerName() != null && !prev.rawPlayerName().isEmpty()
            ? prev.rawPlayerName() : prev.senderName().getString();
        String b = msg.rawPlayerName() != null && !msg.rawPlayerName().isEmpty()
            ? msg.rawPlayerName() : msg.senderName().getString();
        if (!a.equals(b)) return false;
        return msg.time() - prev.time() <= GROUP_TIME_MS;
    }
}
