package com.niuqu.chatbubble.chat.notification;

public final class NotificationSoundGate {
    private static final long COOLDOWN_MS = 2000;
    private static long lastSoundMs = Long.MIN_VALUE;

    private NotificationSoundGate() {}

    public static boolean tryPlay(Runnable sound) {
        long now = System.currentTimeMillis();
        if (now - lastSoundMs < COOLDOWN_MS) return false;
        lastSoundMs = now;
        sound.run();
        return true;
    }
}
