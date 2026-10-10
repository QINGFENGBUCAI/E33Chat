package com.niuqu.chatbubble.chat;

public final class Names {
    private Names() {}

    public static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
