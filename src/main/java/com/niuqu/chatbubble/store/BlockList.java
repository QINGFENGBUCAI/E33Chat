package com.niuqu.chatbubble.store;
import java.util.List;
import net.minecraft.text.Text;

// 玩家屏蔽列表
public final class BlockList {
    // 私有构造
    private BlockList() {}

    // 判断是否匹配屏蔽
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

    // 判断玩家是否屏蔽
    public static boolean isPlayerBlocked(String rawPlayerName, Text senderName, List<? extends String> blocked) {
        if (blocked == null || blocked.isEmpty()) return false;
        if (matchesBlocked(rawPlayerName, blocked)) return true;
        return senderName != null && matchesBlocked(senderName.getString(), blocked);
    }

    // 判断消息是否屏蔽
    public static boolean isBlocked(ChatMessageStore.ChatMessage m, List<? extends String> blocked) {
        return isPlayerBlocked(m.rawPlayerName(), m.senderName(), blocked);
    }

    // 判断消息是否屏蔽
    public static boolean isBlocked(ChatMessageStore.ChatMessage m) {
        return isBlocked(m, com.niuqu.chatbubble.ChatBubbleClientSetup.config().blockedPlayers());
    }
}
