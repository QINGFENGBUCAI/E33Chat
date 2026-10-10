package com.niuqu.chatbubble.store;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.text.Text;

public final class EchoTracker {
    private EchoTracker() {}

    public static final long QUOTE_ECHO_WINDOW_MS = 5_000;
    public static final long REPOST_DEDUP_MS = 1_000;
    private static long lastQuoteSendTime;

    public static boolean isRepostDuplicate(String lastRepostText, long lastRepostTime, String newText, long now) {
        return newText.equals(lastRepostText) && now - lastRepostTime < REPOST_DEDUP_MS;
    }

    private static final Map<String, PendingMeta> pendingMetas = new HashMap<>();

    private static final ThreadLocal<ChatMessageStore.SenderMeta> PENDING_META = new ThreadLocal<>();
    private static long pendingMetaSetTime;

    public static void setPendingMeta(ChatMessageStore.SenderMeta meta) {
        PENDING_META.set(meta);
        pendingMetaSetTime = System.currentTimeMillis();
    }

    public static ChatMessageStore.SenderMeta consumePendingMeta() {
        ChatMessageStore.SenderMeta m = PENDING_META.get();
        PENDING_META.remove();
        if (m != null && System.currentTimeMillis() - pendingMetaSetTime > 2_000) return null;
        return m;
    }

    private record PendingEcho(String text, long time, boolean quoted) {}
    private static final List<PendingEcho> pendingEchoes = new ArrayList<>();
    public record EchoMatch(boolean matched, boolean quoted) {}

    private record PendingWhisperEcho(String target, long time) {}
    private static final Deque<PendingWhisperEcho> pendingWhisperEchoes = new ArrayDeque<>();
    private static long suppressCaptureTime;
    private static boolean suppressQuoted;

    public static void markPendingWhisperEcho(String target) {
        pendingWhisperEchoes.addLast(new PendingWhisperEcho(target, System.currentTimeMillis()));
    }
    public static void markSuppressCapture() {
        suppressCaptureTime = System.currentTimeMillis();

        suppressQuoted = !pendingEchoes.isEmpty() && pendingEchoes.get(pendingEchoes.size() - 1).quoted();
    }
    public static boolean consumeSuppressQuoted() {
        boolean q = suppressQuoted;
        suppressQuoted = false;
        return q;
    }

    static void purgeStaleWhisperEchoes() {
        long cutoff = System.currentTimeMillis() - 10_000;
        while (!pendingWhisperEchoes.isEmpty() && pendingWhisperEchoes.peekFirst().time() < cutoff) {
            pendingWhisperEchoes.pollFirst();
        }
    }

    public static boolean hasPendingWhisperEcho() {
        purgeStaleWhisperEchoes();
        return !pendingWhisperEchoes.isEmpty();
    }
    public static String getPendingWhisperTarget() {
        purgeStaleWhisperEchoes();
        PendingWhisperEcho head = pendingWhisperEchoes.peekFirst();
        return head != null ? head.target() : null;
    }
    public static void consumeWhisperEcho() { pendingWhisperEchoes.pollFirst(); }

    public static boolean consumeSuppressCapture() {
        if (suppressCaptureTime == 0) return false;
        boolean fresh = System.currentTimeMillis() - suppressCaptureTime < 5_000;
        suppressCaptureTime = 0;
        return fresh;
    }

    static void purgeStaleEchoes() {
        long cutoff = System.currentTimeMillis() - 10_000;
        pendingEchoes.removeIf(e -> e.time() < cutoff);
    }

    public static void incrementPendingEcho(String sentText) {
        purgeStaleEchoes();

        boolean quoted = wasRecentQuoteAt(lastQuoteSendTime, System.currentTimeMillis());
        lastQuoteSendTime = 0;
        pendingEchoes.add(new PendingEcho(sentText, System.currentTimeMillis(), quoted));
    }

    public static EchoMatch consumeEchoBySystemChat(String incomingText) {
        purgeStaleEchoes();
        for (int i = 0; i < pendingEchoes.size(); i++) {
            if (incomingText.equals(pendingEchoes.get(i).text())) {
                boolean quoted = pendingEchoes.get(i).quoted();
                pendingEchoes.remove(i);
                return new EchoMatch(true, quoted);
            }
        }
        return new EchoMatch(false, false);
    }

    public static EchoMatch consumeEchoIfSenderMatches(UUID senderUUID, Text senderName, String incomingText) {
        purgeStaleEchoes();
        if (pendingEchoes.isEmpty()) return new EchoMatch(false, false);
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) return new EchoMatch(false, false);

        boolean match = senderUUID != null && senderUUID.equals(player.getUuid());

        if (!match) {
            String s = senderName.getString();
            match = containsWholeName(s, player.getName().getString());
            if (!match && player.networkHandler != null) {
                var info = player.networkHandler.getPlayerListEntry(player.getUuid());
                if (info != null && info.getDisplayName() != null) {
                    String tab = info.getDisplayName().getString().trim();
                    match = !tab.isEmpty() && containsWholeName(s, tab);
                }
            }
        }
        if (match) {

            if (incomingText != null) {
                for (int i = pendingEchoes.size() - 1; i >= 0; i--) {
                    if (incomingText.equals(pendingEchoes.get(i).text())) {
                        boolean quoted = pendingEchoes.get(i).quoted();
                        pendingEchoes.remove(i);
                        ChatMessageStore.updateLatestOwnSenderName(senderName);
                        return new EchoMatch(true, quoted);
                    }
                }
            }

            PendingEcho e = pendingEchoes.remove(0);
            ChatMessageStore.updateLatestOwnSenderName(senderName);
            return new EchoMatch(true, e.quoted());
        }
        return new EchoMatch(false, false);
    }

    public static boolean containsWholeName(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) return false;

        String h = haystack.replaceAll("§.", "");
        String n = needle.replaceAll("§.", "");
        if (n.isEmpty()) return false;
        int from = 0;
        while (true) {
            int idx = h.indexOf(n, from);
            if (idx < 0) return false;
            int end = idx + n.length();
            boolean leftOk = idx == 0 || !isNamePart(h.charAt(idx - 1));
            boolean rightOk = end >= h.length() || !isNamePart(h.charAt(end));
            if (leftOk && rightOk) return true;
            from = idx + 1;
        }
    }

    public static boolean isNamePart(char c) {
        return com.niuqu.chatbubble.chat.Names.isNameChar(c);
    }

    public static void markQuoteSent() {
        lastQuoteSendTime = System.currentTimeMillis();
    }

    public static boolean wasRecentQuoteAt(long quoteSendTime, long now) {
        return quoteSendTime != 0 && now - quoteSendTime < QUOTE_ECHO_WINDOW_MS;
    }

    public static boolean wasRecentQuote() {
        return wasRecentQuoteAt(lastQuoteSendTime, System.currentTimeMillis());
    }

    record PendingMeta(UUID senderUUID, String quoteSender, String quoteContent,
                       List<String> mentionTargets, long createdAt) {}

    public static PendingMeta removePendingMeta(String messageHash) {
        return pendingMetas.remove(messageHash);
    }

    public static void putPendingMeta(String messageHash, UUID senderUUID,
                                      String quoteSender, String quoteContent,
                                      List<String> mentionTargets) {
        pendingMetas.put(messageHash, new PendingMeta(senderUUID, quoteSender, quoteContent,
            mentionTargets, System.currentTimeMillis()));
    }

    public static void prunePendingMetas(long cutoff) {
        pendingMetas.entrySet().removeIf(e -> e.getValue().createdAt() < cutoff);
    }
}
