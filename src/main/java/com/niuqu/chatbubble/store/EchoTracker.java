package com.niuqu.chatbubble.store;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.text.Text;

// 回声跟踪器
public final class EchoTracker {
    // 私有构造
    private EchoTracker() {}

    public static final long QUOTE_ECHO_WINDOW_MS = 5_000;
    public static final long REPOST_DEDUP_MS = 1_000;
    private static long lastQuoteSendTime;

    // 判断转发是否重复
    public static boolean isRepostDuplicate(String lastRepostText, long lastRepostTime, String newText, long now) {
        return newText.equals(lastRepostText) && now - lastRepostTime < REPOST_DEDUP_MS;
    }

    private static final Map<String, PendingMeta> pendingMetas = new HashMap<>();

    private static final ThreadLocal<ChatMessageStore.SenderMeta> PENDING_META = new ThreadLocal<>();
    private static long pendingMetaSetTime;

    // 设置待处理元数据
    public static void setPendingMeta(ChatMessageStore.SenderMeta meta) {
        PENDING_META.set(meta);
        pendingMetaSetTime = System.currentTimeMillis();
    }

    // 取出待处理元数据
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

    // 标记私聊回声
    public static void markPendingWhisperEcho(String target) {
        pendingWhisperEchoes.addLast(new PendingWhisperEcho(target, System.currentTimeMillis()));
    }
    // 标记抑制捕获
    public static void markSuppressCapture() {
        suppressCaptureTime = System.currentTimeMillis();
        suppressQuoted = !pendingEchoes.isEmpty() && pendingEchoes.get(pendingEchoes.size() - 1).quoted();
    }
    // 取出抑制引用标记
    public static boolean consumeSuppressQuoted() {
        boolean q = suppressQuoted;
        suppressQuoted = false;
        return q;
    }

    // 清理过期私聊回声
    static void purgeStaleWhisperEchoes() {
        long cutoff = System.currentTimeMillis() - 10_000;
        while (!pendingWhisperEchoes.isEmpty() && pendingWhisperEchoes.peekFirst().time() < cutoff) {
            pendingWhisperEchoes.pollFirst();
        }
    }

    // 是否存在私聊回声
    public static boolean hasPendingWhisperEcho() {
        purgeStaleWhisperEchoes();
        return !pendingWhisperEchoes.isEmpty();
    }
    // 获取私聊回声目标
    public static String getPendingWhisperTarget() {
        purgeStaleWhisperEchoes();
        PendingWhisperEcho head = pendingWhisperEchoes.peekFirst();
        return head != null ? head.target() : null;
    }
    // 消耗私聊回声
    public static void consumeWhisperEcho() { pendingWhisperEchoes.pollFirst(); }

    // 取出抑制捕获标记
    public static boolean consumeSuppressCapture() {
        if (suppressCaptureTime == 0) return false;
        boolean fresh = System.currentTimeMillis() - suppressCaptureTime < 5_000;
        suppressCaptureTime = 0;
        return fresh;
    }

    // 清理过期回声
    static void purgeStaleEchoes() {
        long cutoff = System.currentTimeMillis() - 10_000;
        pendingEchoes.removeIf(e -> e.time() < cutoff);
    }

    // 记录待匹配回声
    public static void incrementPendingEcho(String sentText) {
        purgeStaleEchoes();
        boolean quoted = wasRecentQuoteAt(lastQuoteSendTime, System.currentTimeMillis());
        lastQuoteSendTime = 0;
        pendingEchoes.add(new PendingEcho(sentText, System.currentTimeMillis(), quoted));
    }

    // 按系统聊天匹配回声
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

    // 按发送者匹配回声
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

    // 判断包含完整名称
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

    // 判断名称字符
    public static boolean isNamePart(char c) {
        return com.niuqu.chatbubble.chat.Names.isNameChar(c);
    }

    // 标记引用发送
    public static void markQuoteSent() {
        lastQuoteSendTime = System.currentTimeMillis();
    }

    // 判断引用是否近期
    public static boolean wasRecentQuoteAt(long quoteSendTime, long now) {
        return quoteSendTime != 0 && now - quoteSendTime < QUOTE_ECHO_WINDOW_MS;
    }

    // 判断是否近期引用
    public static boolean wasRecentQuote() {
        return wasRecentQuoteAt(lastQuoteSendTime, System.currentTimeMillis());
    }

    record PendingMeta(UUID senderUUID, String quoteSender, String quoteContent,
                       List<String> mentionTargets, long createdAt) {}

    // 取出待处理元数据
    public static PendingMeta removePendingMeta(String messageHash) {
        return pendingMetas.remove(messageHash);
    }

    // 存入待处理元数据
    public static void putPendingMeta(String messageHash, UUID senderUUID,
                                      String quoteSender, String quoteContent,
                                      List<String> mentionTargets) {
        pendingMetas.put(messageHash, new PendingMeta(senderUUID, quoteSender, quoteContent,
            mentionTargets, System.currentTimeMillis()));
    }

    // 清理过期元数据
    public static void prunePendingMetas(long cutoff) {
        pendingMetas.entrySet().removeIf(e -> e.getValue().createdAt() < cutoff);
    }
}
