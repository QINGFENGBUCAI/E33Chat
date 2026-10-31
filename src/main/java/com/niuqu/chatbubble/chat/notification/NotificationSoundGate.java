package com.niuqu.chatbubble.chat.notification;

// 通知音效节流
public final class NotificationSoundGate {
    private static final long COOLDOWN_MS = 2000;
    private static long lastSoundMs = Long.MIN_VALUE;

    // 私有构造
    private NotificationSoundGate() {}

    // 尝试播放音效
    public static boolean tryPlay(Runnable sound) {
        long now = System.currentTimeMillis();
        if (now - lastSoundMs < COOLDOWN_MS) return false;
        lastSoundMs = now;
        sound.run();
        return true;
    }
}
