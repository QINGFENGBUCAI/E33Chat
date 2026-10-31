package com.niuqu.chatbubble.chat;

// 名字字符判断
public final class Names {
    // 私有构造
    private Names() {}

    // 是否名字字符
    public static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
