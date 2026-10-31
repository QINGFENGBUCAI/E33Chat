package com.niuqu.chatbubble.chat;

// 提及检测
public final class MentionDetector {
    // 私有构造
    private MentionDetector() {}

    // 是否被提及
    public static boolean isMentioned(String text, String localPlayerName,
                                      boolean requireAt, String replySender) {
        if (text == null || localPlayerName == null || localPlayerName.isBlank()) return false;
        String lower = text.toLowerCase();
        String needle = localPlayerName.toLowerCase();
        int idx = 0;
        while ((idx = lower.indexOf(needle, idx)) >= 0) {
            int end = idx + needle.length();
            boolean hasAt = idx > 0 && text.charAt(idx - 1) == '@';
            if (hasAt) {
                if (end >= text.length() || !isNameCharacter(text.charAt(end))) {
                    return true;
                }
            } else if (!requireAt) {
                boolean leftOk = idx == 0 || !isNameCharacter(text.charAt(idx - 1));
                boolean rightOk = end >= text.length() || !isNameCharacter(text.charAt(end));
                if (leftOk && rightOk) return true;
            }
            idx = end;
        }
        if (replySender != null && replySender.equals(localPlayerName)) return true;
        return false;
    }

    // 是否名字字符
    private static boolean isNameCharacter(char c) {
        return Names.isNameChar(c);
    }
}
