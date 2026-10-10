package com.niuqu.chatbubble.chat;

import java.util.regex.Pattern;

public final class WhisperSignal {
    private WhisperSignal() {}

    public static final String[] ZH = {
        "悄悄", "whisper", "对你说", "私聊", "密语", "密聊", "私信", "密谈"
    };

    public static final Pattern EN = Pattern.compile("\\b(?:pm|message|msg|tell)\\b");

    public static boolean containsZh(String text) {
        if (text == null) return false;
        for (String w : ZH) {
            if (text.contains(w)) return true;
        }
        return false;
    }
}
