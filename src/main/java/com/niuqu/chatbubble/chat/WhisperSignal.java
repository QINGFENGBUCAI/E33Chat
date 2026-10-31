package com.niuqu.chatbubble.chat;
import java.util.regex.Pattern;

// 私聊信号常量
public final class WhisperSignal {
    // 私有构造
    private WhisperSignal() {}

    public static final String[] ZH = {
        "悄悄", "whisper", "对你说", "私聊", "密语", "密聊", "私信", "密谈"
    };

    public static final Pattern EN = Pattern.compile("\\b(?:pm|message|msg|tell)\\b");

    // 是否含中文私聊词
    public static boolean containsZh(String text) {
        if (text == null) return false;
        for (String w : ZH) {
            if (text.contains(w)) return true;
        }
        return false;
    }
}
