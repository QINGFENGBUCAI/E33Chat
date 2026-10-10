package com.niuqu.chatbubble.store;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.E33Log;
import com.niuqu.chatbubble.GuiCompat;
import com.niuqu.chatbubble.config.ChatBubbleConfig;

import net.minecraft.util.Formatting;import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatMessageStore {

    public interface MessageEffectObserver {
        void onMentionOrQuote(Text content, SenderMeta meta, int index, String replySender);
        void onWhisperReceived(UUID senderUUID, Text senderName, Text content, int index);
        void onSystemMessage(Text content, int index);
        void onPublicChatSound();
        void onQuoteSound();
    }

    private static final MessageEffectObserver NOOP_OBSERVER = new MessageEffectObserver() {
        @Override public void onMentionOrQuote(Text content, SenderMeta meta, int index, String replySender) {}
        @Override public void onWhisperReceived(UUID senderUUID, Text senderName, Text content, int index) {}
        @Override public void onSystemMessage(Text content, int index) {}
        @Override public void onPublicChatSound() {}
        @Override public void onQuoteSound() {}
    };
    private static MessageEffectObserver effectObserver = NOOP_OBSERVER;

    public static void setMessageEffectObserver(MessageEffectObserver observer) {
        effectObserver = observer != null ? observer : NOOP_OBSERVER;
    }

    private static final int MAX = 10000;
    private static final List<ChatMessage> messages = new ArrayList<>();
    private static int unreadCount = 0;
    private static boolean hasUnreadMentionFlag;
    private static boolean screenOpen = false;
    private static String pendingReplyContent;
    private static String pendingReplySender;

    public static boolean isRepostDuplicate(String lastRepostText, long lastRepostTime, String newText, long now) {
        return EchoTracker.isRepostDuplicate(lastRepostText, lastRepostTime, newText, now);
    }

    private static String currentWorldKey;

    public record SeenPlayer(UUID uuid, String profileName, String displayName) {}

    private static final int SEEN_PLAYERS_CAP = 512;
    private static final Map<UUID, SeenPlayer> seenPlayers = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, SeenPlayer> eldest) {
            return size() > SEEN_PLAYERS_CAP;
        }
    };

    private static volatile boolean serverUseTpa = false;
    public static void setServerUseTpa(boolean v) { serverUseTpa = v; }
    public static boolean useTpa() { return serverUseTpa; }

    private static volatile List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverChatTemplates = List.of();
    private static volatile List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverWhisperTemplates = List.of();
    private static volatile boolean serverTemplateDebug = false;

    private static volatile boolean easyBotCompat = true;

    public static void setEasyBotCompat(boolean v) { easyBotCompat = v; }
    public static boolean isEasyBotCompat() { return easyBotCompat; }

    public static void setServerConfig(boolean useTpa, List<String> chatTemplates,
                                       List<String> whisperTemplates, boolean templateDebug) {
        serverUseTpa = useTpa;
        serverChatTemplates = compileTemplates(chatTemplates);
        serverWhisperTemplates = compileTemplates(whisperTemplates);
        serverTemplateDebug = templateDebug;
    }

    private static List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> compileTemplates(List<String> raws) {
        if (raws == null || raws.isEmpty()) return List.of();
        List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> out = new ArrayList<>();
        for (String raw : raws) {
            var result = com.niuqu.chatbubble.chat.TemplateMatcher.compile(raw);
            if (result.template() != null) {
                out.add(result.template());
                if (!result.template().unknownFields().isEmpty()) {
                    debugLog(() -> "[e33chat] template has unknown placeholders (treated as literal): "
                        + result.template().unknownFields() + " | template='" + raw + "'");
                }
            } else {
                debugLog(() -> "[e33chat] server template skipped: '" + raw + "' -> " + result.error());
            }
        }
        return out;
    }

    public static List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverChatTemplates() {
        return serverChatTemplates;
    }

    public static List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverWhisperTemplates() {
        return serverWhisperTemplates;
    }

    public static boolean serverTemplateDebug() { return serverTemplateDebug; }

    public static void rememberPlayer(UUID uuid, String profileName, String displayName) {
        if (uuid == null || uuid.equals(new UUID(0, 0)) || profileName == null || profileName.isEmpty()) return;
        SeenPlayer existing = seenPlayers.get(uuid);
        String newDisplay = (displayName != null && !displayName.isEmpty()) ? displayName
            : (existing != null ? existing.displayName() : null);
        seenPlayers.put(uuid, new SeenPlayer(uuid, profileName, newDisplay));
    }

    public static List<String> knownNameVariants() {
        Set<String> out = new LinkedHashSet<>();
        for (SeenPlayer sp : seenPlayers.values()) {
            if (sp.profileName() != null && !sp.profileName().isEmpty()) {
                out.add(sp.profileName());
                String stripped = sp.profileName().replaceAll("§.", "");
                if (!stripped.isEmpty()) out.add(stripped);
            }
            if (sp.displayName() != null && !sp.displayName().isEmpty()) {
                out.add(sp.displayName());
                String stripped = sp.displayName().replaceAll("§.", "");
                if (!stripped.isEmpty()) out.add(stripped);
            }
        }
        return new ArrayList<>(out);
    }

    public static UUID findSeenUuid(String name) {
        if (name == null || name.isEmpty()) return null;
        String stripped = name.replaceAll("§.", "");
        for (SeenPlayer sp : seenPlayers.values()) {
            if (matchesSeenName(name, stripped, sp.profileName())) return sp.uuid();
            if (matchesSeenName(name, stripped, sp.displayName())) return sp.uuid();
        }
        return null;
    }

    private static boolean matchesSeenName(String raw, String stripped, String stored) {
        if (stored == null || stored.isEmpty()) return false;
        if (raw.equals(stored)) return true;
        if (!stripped.isEmpty() && stripped.equals(stored)) return true;
        String storedStripped = stored.replaceAll("§.", "");
        return raw.equals(storedStripped) || (!stripped.isEmpty() && stripped.equals(storedStripped));
    }

    public static boolean isKnownPlayerName(String name) {
        if (name == null || name.isEmpty()) return false;
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) return false;
        String myName = player.getName().getString();
        if (!myName.isEmpty() && (name.equals(myName) || name.contains(myName))) return true;
        if (player.networkHandler != null) {
            for (var info : player.networkHandler.getPlayerList()) {
                String profile = GuiCompat.profileName(info.getProfile());
                if (!profile.isEmpty() && (name.equals(profile) || name.contains(profile))) return true;
                var tab = info.getDisplayName();
                if (tab != null) {
                    String ts = tab.getString();
                    if (!ts.isEmpty() && (name.equals(ts) || name.contains(ts))) return true;
                }
            }
        }
        return findSeenUuid(name) != null;
    }

    public record SenderMeta(UUID senderUUID, Text senderName,
                             Text rawContent, boolean isSystem,
                             String rawPlayerName,
                             boolean whisper, String whisperPartner) {}

    public static void setPendingMeta(SenderMeta meta) {
        EchoTracker.setPendingMeta(meta);
    }

    public static SenderMeta consumePendingMeta() {
        return EchoTracker.consumePendingMeta();
    }

    public static void markPendingWhisperEcho(String target) {
        EchoTracker.markPendingWhisperEcho(target);
    }

    public static void markSuppressCapture() {
        EchoTracker.markSuppressCapture();
    }

    public static boolean consumeSuppressQuoted() {
        return EchoTracker.consumeSuppressQuoted();
    }

    private static void purgeStaleWhisperEchoes() {
        EchoTracker.purgeStaleWhisperEchoes();
    }

    public static boolean hasPendingWhisperEcho() {
        return EchoTracker.hasPendingWhisperEcho();
    }

    public static String getPendingWhisperTarget() {
        return EchoTracker.getPendingWhisperTarget();
    }

    public static void consumeWhisperEcho() {
        EchoTracker.consumeWhisperEcho();
    }

    public static boolean consumeSuppressCapture() {
        return EchoTracker.consumeSuppressCapture();
    }

    private static void purgeStaleEchoes() {
        EchoTracker.purgeStaleEchoes();
    }

    public static void incrementPendingEcho(String sentText) {
        EchoTracker.incrementPendingEcho(sentText);
    }

    public static EchoTracker.EchoMatch consumeEchoBySystemChat(String incomingText) {
        return EchoTracker.consumeEchoBySystemChat(incomingText);
    }

    public static void debugLog(java.util.function.Supplier<String> msg) {
        if (ChatBubbleClientSetup.config().debugLog())
            E33Log.info(msg.get());
    }

    public static void debugLog(String msg) {
        debugLog(() -> msg);
    }

    public static EchoTracker.EchoMatch consumeEchoIfSenderMatches(UUID senderUUID, Text senderName, String incomingText) {
        return EchoTracker.consumeEchoIfSenderMatches(senderUUID, senderName, incomingText);
    }

    public static boolean containsWholeName(String haystack, String needle) {
        return EchoTracker.containsWholeName(haystack, needle);
    }

    public static boolean isNamePart(char c) {
        return EchoTracker.isNamePart(c);
    }

    public static void updateLatestOwnSenderName(Text senderName) {
        for (int i = messages.size() - 1; i >= 0 && i >= messages.size() - 5; i--) {
            ChatMessage m = messages.get(i);
            if (!m.isOwn()) continue;
            if (!m.senderName().getString().equals(senderName.getString())) {
                messages.set(i, m.withSenderName(senderName));
                bumpListVersion();
            }
            return;
        }
    }

    public static boolean isRecentDuplicate(String content) {
        int size = messages.size();
        for (int i = size - 1; i >= 0 && i >= size - 2; i--) {
            if (messages.get(i).content().getString().equals(content)) return true;
        }
        return false;
    }

    public record ChatMessage(
        UUID senderUUID,
        Text senderName,
        Text content,
        long time,
        boolean isOwn,
        boolean isSystem,
        String replyContent,
        String replySender,
        String messageHash,
        int duplicateCount,
        String rawPlayerName,
        boolean whisper,
        String whisperPartner,
        String group
    ) {

        public ChatMessage withSenderName(Text newSenderName) {
            return new ChatMessage(senderUUID, newSenderName, content, time,
                isOwn, isSystem, replyContent, replySender, messageHash, duplicateCount,
                rawPlayerName, whisper, whisperPartner, group);
        }

        public ChatMessage withMerge(String newReplyContent, String newReplySender, long newTime, int newDupCount) {
            return new ChatMessage(senderUUID, senderName, content, newTime,
                isOwn, isSystem, newReplyContent, newReplySender, messageHash, newDupCount,
                rawPlayerName, whisper, whisperPartner, group);
        }

        public ChatMessage withReply(String newReplyContent, String newReplySender) {
            return new ChatMessage(senderUUID, senderName, content, time,
                isOwn, isSystem, newReplyContent, newReplySender, messageHash, duplicateCount,
                rawPlayerName, whisper, whisperPartner, group);
        }
    }

    private static boolean isSameSender(ChatMessage last, Text senderName, String rawPlayerName) {
        if (rawPlayerName != null && !rawPlayerName.isEmpty()
            && last.rawPlayerName() != null && !last.rawPlayerName().isEmpty()) {
            return rawPlayerName.equals(last.rawPlayerName());
        }
        return last.senderName().getString().equals(senderName.getString());
    }

    public static void purgeBlocked(List<? extends String> blocked) {
        if (blocked == null || blocked.isEmpty()) return;
        messages.removeIf(m -> !m.isOwn() && !m.isSystem()
            && BlockList.isPlayerBlocked(m.rawPlayerName(), m.senderName(), blocked));
        bumpListVersion();
    }

    private static boolean isBlockedMessage(ChatMessage m) {
        return BlockList.isPlayerBlocked(m.rawPlayerName(), m.senderName(),
            ChatBubbleClientSetup.config().blockedPlayers());
    }

    public static java.util.function.Supplier<net.minecraft.entity.player.PlayerEntity> localPlayerSupplier =
        () -> net.minecraft.client.MinecraftClient.getInstance().player;

    public static void addMessage(Text content, UUID senderUUID, Text senderName, boolean isSystem, String rawPlayerName, boolean whisper, String whisperPartner, boolean localSend) {
        addMessage(content, senderUUID, senderName, isSystem, rawPlayerName, whisper, whisperPartner, localSend, null);
    }

    public static void addGroupMessage(Text content, UUID senderUUID, Text senderName,
                                       String group, String quoteSender, String quoteContent) {
        if (group != null && !group.isEmpty()) {
            EchoTracker.putPendingMeta(
                String.valueOf(content.getString().hashCode()),
                senderUUID, quoteSender != null ? quoteSender : "",
                quoteContent != null ? quoteContent : "",
                java.util.List.of());
        }
        addMessage(content, senderUUID, senderName, false, senderName != null ? senderName.getString() : null,
            false, null, false, group);
    }

    public static void addMessage(Text content, UUID senderUUID, Text senderName, boolean isSystem, String rawPlayerName, boolean whisper, String whisperPartner, boolean localSend, String group) {
        String messageHash = String.valueOf(content.getString().hashCode());

        if (content.getString().isBlank()) return;

        var localPlayer = localPlayerSupplier.get();
        String playerName = localPlayer != null ? localPlayer.getName().getString() : "";

        boolean own = localPlayer != null && senderUUID != null
            && senderUUID.equals(localPlayer.getUuid());
        if (!own) {
            own = (rawPlayerName != null && !rawPlayerName.isEmpty())
                ? rawPlayerName.equals(playerName)
                : senderName != null && senderName.getString().equals(playerName);
        }

        if (own) cacheOwnDecoratedName(senderName);

        if (ChatBubbleClientSetup.config().antiSpam() && !messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            if (!last.isSystem() && isSameSender(last, senderName, rawPlayerName)
                && last.content().getString().equals(content.getString())) {

                EchoTracker.PendingMeta pending = EchoTracker.removePendingMeta(messageHash);
                if (pending != null && System.currentTimeMillis() - pending.createdAt() > 10_000) {
                    pending = null;
                }
                String mergeReplyContent = null;
                String mergeReplySender = null;
                if (own && pendingReplyContent != null) {
                    mergeReplyContent = pendingReplyContent;
                    mergeReplySender = pendingReplySender;
                } else if (pending != null && !pending.quoteContent().isEmpty()) {
                    mergeReplyContent = pending.quoteContent();
                    mergeReplySender = pending.quoteSender();
                }
                pendingReplyContent = null;
                pendingReplySender = null;
                messages.set(messages.size() - 1, last.withMerge(
                    mergeReplyContent, mergeReplySender, System.currentTimeMillis(),
                    last.duplicateCount() + 1));
                bumpListVersion();
                return;
            }
        }

        EchoTracker.PendingMeta pending = EchoTracker.removePendingMeta(messageHash);
        if (pending != null && System.currentTimeMillis() - pending.createdAt() > 10_000) {
            pending = null;
        }

        String replyContent = null;
        String replySender = null;
        if (own && pendingReplyContent != null) {
            replyContent = pendingReplyContent;
            replySender = pendingReplySender;
            pendingReplyContent = null;
            pendingReplySender = null;
        } else if (pending != null && !pending.quoteContent().isEmpty()) {
            replyContent = pending.quoteContent();
            replySender = pending.quoteSender();
        }

        messages.add(new ChatMessage(
            senderUUID,
            senderName != null ? senderName : Text.literal(""),
            content,
            System.currentTimeMillis(),
            own,
            isSystem,
            replyContent,
            replySender,
            messageHash,
            1,
            rawPlayerName,
            whisper,
            whisperPartner,
            group
        ));

        if (!isSystem && senderUUID != null && !senderUUID.equals(new UUID(0, 0)))
            rememberPlayer(senderUUID, rawPlayerName, senderName.getString());

        while (messages.size() > MAX)
            messages.remove(0);
        bumpListVersion();
        historyDirty = true;

        boolean isMentionOrQuote = !isSystem
            && com.niuqu.chatbubble.chat.MentionDetector.isMentioned(
                content.getString(), playerName,
                ChatBubbleClientSetup.config().mentionRequireAt(), replySender);

        if (isMentionOrQuote) {
            if (!screenOpen) hasUnreadMentionFlag = true;
            effectObserver.onMentionOrQuote(
                content, new SenderMeta(senderUUID, senderName, content, isSystem,
                    rawPlayerName, whisper, whisperPartner),
                messages.size(), replySender);
        }

        if (whisper && rawPlayerName != null
            && ChatBubbleClientSetup.config().mentionWhisperBanner()
            && (!localSend || ChatBubbleClientSetup.config().ownWhisperNotify())) {
            effectObserver.onWhisperReceived(
                senderUUID, senderName, content, messages.size());
        }

        if (isSystem && ChatBubbleClientSetup.config().systemBannerEnabled()) {
            effectObserver.onSystemMessage(content, messages.size());
        }

        if (!own && localPlayerSupplier.get() != null && !isMentionOrQuote && !whisper
            && (isSystem ? ChatBubbleClientSetup.config().soundSystem()
                         : ChatBubbleClientSetup.config().soundPublic())) {
            effectObserver.onPublicChatSound();
        }

        if (!screenOpen) {
            unreadCount++;
        }

        if (whisper && whisperPartner != null && !own) {
            markWhisperUnread(whisperPartner);
        }
    }

    public static Text sliceStyled(Text src, int start, int end) {
        MutableText out = Text.empty();
        int[] pos = {0};
        src.visit((style, text) -> {
            int s = pos[0], e = s + text.length();
            pos[0] = e;
            int from = Math.max(start, s), to = Math.min(end, e);
            if (from < to)
                out.append(Text.literal(text.substring(from - s, to - s)).fillStyle(style));
            return Optional.<Object>empty();
        }, Style.EMPTY);
        return out;
    }

    public static String singleLine(String s) {
        return stripControls(s);
    }

    private static String stripControls(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c < 0x20 || c == 0x7F ? ' ' : c);
        }
        return sb.toString();
    }

    public static List<ChatMessage> getMessages() {
        return messages;
    }

    private static int listVersion = 0;
    private static void bumpListVersion() { listVersion++; }

    private static int publicCacheVersion = -1;
    private static List<ChatMessage> publicCache = List.of();

    private static final Map<String, List<ChatMessage>> whisperCache = new HashMap<>();
    private static int whisperCacheVersion = -1;

    public static int listVersion() { return listVersion; }

    public static List<ChatMessage> getWhisperMessages(String partnerName) {
        if (whisperCacheVersion != listVersion) {
            whisperCache.clear();
            whisperCacheVersion = listVersion;
        }
        List<ChatMessage> cached = whisperCache.get(partnerName);
        if (cached != null) return cached;
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.whisper() && partnerName.equals(msg.whisperPartner())) {
                result.add(msg);
            }
        }

        if (whisperCache.size() > 64) whisperCache.clear();
        whisperCache.put(partnerName, result);
        return result;
    }

    public static List<ChatMessage> getPublicMessages() {
        if (publicCacheVersion == listVersion) return publicCache;
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (!msg.whisper()) {
                result.add(msg);
            }
        }
        publicCache = result;
        publicCacheVersion = listVersion;
        return result;
    }

    public static ChatMessage getLatestWhisperWith(String partnerName) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.whisper() && partnerName.equals(msg.whisperPartner())) {
                return msg;
            }
        }
        return null;
    }

    public static ChatMessage getLatestPublicMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (!msg.whisper()) {
                return msg;
            }
        }
        return null;
    }

    private static final Set<String> unreadWhisperPartners = new java.util.HashSet<>();

    public static void markWhisperUnread(String partner) {
        if (partner != null) unreadWhisperPartners.add(partner);
    }

    public static void clearUnreadWhisper(String partner) {
        unreadWhisperPartners.remove(partner);
    }

    public static boolean hasUnreadWhisper(String partner) {
        return unreadWhisperPartners.contains(partner);
    }

    public static int getUnreadCount() {
        return unreadCount;
    }

    public static void markAllRead() {
        unreadCount = 0;
        hasUnreadMentionFlag = false;
    }

    public static void setScreenOpen(boolean open) {
        screenOpen = open;
        if (open) {
            unreadCount = 0;
            hasUnreadMentionFlag = false;
        }
    }

    public static boolean hasUnreadMention(String playerName) {
        return hasUnreadMentionFlag;
    }

    public static Text quoteMessage(int index) {
        if (index < 0 || index >= messages.size()) return Text.literal("");
        ChatMessage msg = messages.get(index);
        String qName = (msg.rawPlayerName() != null && !msg.rawPlayerName().isEmpty())
            ? msg.rawPlayerName() : msg.senderName().getString();
        MutableText quote = Text.literal("> " + qName + ": ");
        quote.append(msg.content());
        return quote;
    }

    public static ChatMessage getMessageAt(int index) {
        if (index < 0 || index >= messages.size()) return null;
        return messages.get(index);
    }

    public static void setPendingReply(String content, String sender) {
        pendingReplyContent = content;
        pendingReplySender = sender;
        EchoTracker.markQuoteSent();
    }

    public static String getPendingReplySender() { return pendingReplySender; }

    public static String timeKey(long timeMillis, int interval) {
        if (interval <= 0) return "";
        return String.valueOf(timeMillis / (interval * 60_000L));
    }

    public static String formatTime(long timeMillis) {
        var dt = java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        java.time.LocalDate today = java.time.LocalDate.now();
        if (dt.toLocalDate().equals(today)) return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (dt.getYear() == today.getYear()) return dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public static boolean wasRecentQuoteAt(long quoteSendTime, long now) {
        return EchoTracker.wasRecentQuoteAt(quoteSendTime, now);
    }

    public static boolean wasRecentQuote() {
        return EchoTracker.wasRecentQuote();
    }

    public static String extractWhisperContent(String text, SenderMeta meta) {
        if (meta != null && meta.rawContent() != null) {
            String rc = meta.rawContent().getString();
            if (!rc.isBlank()) return rc;
        }
        int idx = text.indexOf(": ");
        if (idx < 0) idx = text.indexOf("：");
        if (idx < 0) return text;
        int start = idx + 1;
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
        return text.substring(start).trim();
    }

    public static Text extractWhisperDisplayName(Text fullLine, Text fallback) {
        String fullStr = fullLine.getString();

        int qiaoIdx = fullStr.indexOf("悄悄地对你说");
        if (qiaoIdx > 0) {
            Text area = sliceStyled(fullLine, 0, qiaoIdx);
            if (!area.getString().isBlank()) return stripItalic(area);
        }

        int duiIdx = fullStr.indexOf("悄悄地对");
        if (duiIdx >= 0) {
            int sayIdx = fullStr.indexOf("说：", duiIdx);
            if (sayIdx > duiIdx) {
                String prefix = fullStr.substring(0, duiIdx).trim();
                if (!prefix.isEmpty() && !prefix.equals("你")) {
                    Text area = sliceStyled(fullLine, fullStr.indexOf(prefix), fullStr.indexOf(prefix) + prefix.length());
                    if (!area.getString().isBlank()) return stripItalic(area);
                }
                return fallback;
            }
        }

        int toIdx = fullStr.indexOf("whisper to ");
        if (toIdx >= 0) {
            int colonIdx = fullStr.indexOf(":", toIdx);
            if (colonIdx > toIdx) {
                String prefix = fullStr.substring(0, toIdx).trim();
                if (!prefix.isEmpty() && !prefix.equalsIgnoreCase("you")) {
                    Text area = sliceStyled(fullLine, fullStr.indexOf(prefix), fullStr.indexOf(prefix) + prefix.length());
                    if (!area.getString().isBlank()) return stripItalic(area);
                }
                return fallback;
            }
        }
        int whisperIdx = fullStr.indexOf(" whispers to you");
        if (whisperIdx > 0) {
            Text area = sliceStyled(fullLine, 0, whisperIdx);
            if (!area.getString().isBlank()) return stripItalic(area);
        }
        return fallback;
    }

    private static Text stripItalic(Text src) {
        MutableText out = Text.empty();
        src.visit((style, text) -> {
            out.append(Text.literal(text).fillStyle(style.withItalic(false)));
            return java.util.Optional.<Object>empty();
        }, net.minecraft.text.Style.EMPTY);
        return out;
    }

    private static Text ownDecoratedName;

    public static Text ownDisplayName() {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null && player.networkHandler != null) {
            var info = player.networkHandler.getPlayerListEntry(player.getUuid());
            if (info != null && info.getDisplayName() != null) {
                return info.getDisplayName();
            }
        }
        if (ownDecoratedName != null) return ownDecoratedName;
        if (player != null && player.getScoreboardTeam() != null) {
            var team = player.getScoreboardTeam();
            //#if MC >= 26000

            Text pfx = team.getPlayerPrefix();
            Text sfx = team.getPlayerSuffix();
            Formatting col = team.getColor();
            //#else
            //$$ Text pfx = team.getPrefix();
            //$$ Text sfx = team.getSuffix();
            //$$ Formatting col = team.getColor();
            //#endif
            boolean hasPfx = pfx != null && !pfx.getString().isEmpty();
            boolean hasSfx = sfx != null && !sfx.getString().isEmpty();
            if (hasPfx || hasSfx || col != null) {
                MutableText name = Text.literal(player.getName().getString());
                if (col != null) name = name.formatted(col);
                MutableText out = Text.empty();
                if (hasPfx) out.append(pfx);
                out.append(name);
                if (hasSfx) out.append(sfx);
                return out;
            }
        }
        return player != null ? player.getName() : Text.literal("?");
    }

    public static Text cachedOwnDisplayName() {
        return ownDecoratedName;
    }

    public static void cacheOwnDecoratedName(Text senderName) {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        String bare = player != null ? player.getName().getString() : "";
        if (senderName == null || bare.isEmpty()) return;
        String sn = senderName.getString();
        if (!sn.isEmpty() && !sn.equals(bare)) ownDecoratedName = senderName;
    }

    public static int size() {
        return messages.size();
    }

    public static void setCurrentWorld(String name) {
        if (java.util.Objects.equals(name, currentWorldKey)) return;
        boolean wasFallback = "world".equals(currentWorldKey);
        boolean isSpecific = name != null && (name.startsWith("SP:") || name.startsWith("MP:"));
        boolean isRefinement = wasFallback && isSpecific;
        boolean hasPendingMessages = currentWorldKey == null && isSpecific && !messages.isEmpty();
        if (ChatBubbleClientSetup.config().chatHistoryEnabled() && isWorldSpecific(currentWorldKey))
            saveMessages(currentWorldKey);
        currentWorldKey = name;
        cleanupOldHistory();
        if (isRefinement || hasPendingMessages) {
            hasUnreadMentionFlag = false;
            if (ChatBubbleClientSetup.config().chatHistoryEnabled() && isWorldSpecific(currentWorldKey)) {

                List<ChatMessage> early = new ArrayList<>(messages);

                Map<String, Integer> skipKeys = usableSkips(backlogKeys, mergeCountsOf(early));
                backlogKeys.clear();
                messages.clear();
                loadMessages(currentWorldKey, skipKeys);
                messages.addAll(early);
                bumpListVersion();
            }
            return;
        }
        messages.clear();
        backlogKeys.clear();
        bumpListVersion();
        unreadCount = 0;
        hasUnreadMentionFlag = false;
        if (ChatBubbleClientSetup.config().chatHistoryEnabled() && isWorldSpecific(currentWorldKey))
            loadMessages(currentWorldKey, java.util.Collections.emptyMap());
    }

    private static boolean isWorldSpecific(String key) {
        return key != null && (key.startsWith("SP:") || key.startsWith("MP:"));
    }

    private static File getHistoryFile(String worldKey) {
        return HistoryStore.getHistoryFile(worldKey);
    }

    private static File getLegacyHistoryFile(String worldKey) {
        return HistoryStore.getLegacyHistoryFile(worldKey);
    }

    private static String sha256Short(String s) {
        return HistoryStore.sha256Short(s);
    }

    public static boolean isSensitiveCommand(String text) {
        return HistoryStore.isSensitiveCommand(text);
    }

    public static String toLine(ChatMessage msg) {
        return HistoryStore.toLine(msg);
    }

    public static ChatMessage fromLine(String line) {
        return HistoryStore.fromLine(line);
    }

    private static ChatMessage fromJsonLine(String line) {
        return HistoryStore.fromJsonLine(line);
    }

    private static Text componentFrom(Map<String, Object> obj, String jsonKey, String textKey) {
        return HistoryStore.componentFrom(obj, jsonKey, textKey);
    }

    private static String escapeField(String s) {
        return HistoryStore.escapeField(s);
    }

    private static String unescapeField(String s) {
        return HistoryStore.unescapeField(s);
    }

    public static Text parseStyledText(String s) {
        return HistoryStore.parseStyledText(s);
    }

    private static Style applySectionCode(Style style, char code) {
        return HistoryStore.applySectionCode(style, code);
    }

    private static List<ChatMessage> loadLegacyFile(File f) {
        return HistoryStore.loadLegacyFile(f);
    }

    private static final java.util.concurrent.ExecutorService SAVE_EXECUTOR =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "e33chat-history-save");
            t.setDaemon(true);
            return t;
        });

    private static void saveMessages(String worldKey) {
        if (messages.isEmpty()) return;
        List<ChatMessage> snapshot = new ArrayList<>(messages);
        long gen = historyGeneration;
        File f = getHistoryFile(worldKey);
        SAVE_EXECUTOR.execute(() -> {

            if (gen != historyGeneration) return;
            f.getParentFile().mkdirs();

            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
                for (ChatMessage msg : snapshot) {
                    String line = toLine(msg);
                    if (line == null) continue;
                    w.write(line);
                    w.write("\n");
                }
                w.flush();
            } catch (Exception e) {
                E33Log.warn("[e33chat] Failed to read/write chat history", e);
                return;
            }

            if (gen != historyGeneration) {
                tmp.delete();
                return;
            }
            try {
                java.nio.file.Files.move(tmp.toPath(), f.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                try {
                    java.nio.file.Files.move(tmp.toPath(), f.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e2) {
                    E33Log.warn("[e33chat] Failed to read/write chat history", e2);
                }
            }
        });
    }

    public static boolean hasHistoryToClear() {
        if (!messages.isEmpty()) return true;
        if (currentWorldKey == null) return false;
        return getHistoryFile(currentWorldKey).exists()
            || getLegacyHistoryFile(currentWorldKey).exists();
    }

    public static void clearCurrentWorldHistory() {
        messages.clear();
        backlogKeys.clear();
        bumpListVersion();
        unreadCount = 0;
        hasUnreadMentionFlag = false;
        unreadWhisperPartners.clear();
        historyDirty = false;
        historyGeneration++;

        if (currentWorldKey == null) return;
        File f = getHistoryFile(currentWorldKey);
        File legacy = getLegacyHistoryFile(currentWorldKey);
        if (f.exists()) f.delete();
        if (legacy.exists()) legacy.delete();

        final long gen = historyGeneration;
        final File cur = f;
        final File leg = legacy;
        SAVE_EXECUTOR.execute(() -> {
            if (gen != historyGeneration) return;
            if (cur.exists()) cur.delete();
            if (leg.exists()) leg.delete();
        });
    }

    private static final long AUTO_SAVE_MS = 30_000;
    private static long lastAutoSave;
    private static boolean historyDirty;

    private static volatile long historyGeneration;

    public static boolean isExpired(long fileMtime, long now, int retentionDays) {
        return HistoryStore.isExpired(fileMtime, now, retentionDays);
    }

    private static void cleanupOldHistory() {
        int days = ChatBubbleClientSetup.config().historyRetentionDays();
        if (days <= 0) return;
        File dir = new File(MinecraftClient.getInstance().runDirectory, "e33chat/history");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        long now = System.currentTimeMillis();
        File current = currentWorldKey != null ? getHistoryFile(currentWorldKey) : null;
        for (File f : files) {
            if (f.equals(current)) continue;
            if (isExpired(f.lastModified(), now, days)) {
                E33Log.info("[e33chat] History retention: deleting " + f.getName());
                f.delete();
            }
        }
    }

    public static void maybeAutoSave() {
        long now = System.currentTimeMillis();
        if (currentWorldKey == null || !historyDirty || now - lastAutoSave < AUTO_SAVE_MS) return;
        historyDirty = false;
        lastAutoSave = now;
        saveMessages(currentWorldKey);
    }

    private static void loadMessages(String worldKey, Map<String, Integer> skipCounts) {

        skipCounts = skipCounts.isEmpty() ? java.util.Collections.<String, Integer>emptyMap()
            : new HashMap<>(skipCounts);
        File f = getHistoryFile(worldKey);
        if (!f.exists()) {
            File legacy = getLegacyHistoryFile(worldKey);
            if (legacy.exists()) f = legacy;
        }
        if (!f.exists()) return;

        new File(f.getParentFile(), f.getName() + ".tmp").delete();
        String head;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            head = br.readLine();
        } catch (Exception e) {
            E33Log.warn("[e33chat] Failed to read/write chat history", e);
            return;
        }
        if (head == null) return;

        if (head.startsWith("﻿")) head = head.substring(1);

        if (head.trim().startsWith("[")) {
            List<ChatMessage> legacy = loadLegacyFile(f);
            for (ChatMessage m : legacy) {
                if (BlockList.isBlocked(m)) continue;

                if (!skipCounts.isEmpty() && !takeFresh(skipCounts, mergeKeyOf(m))) continue;
                messages.add(m);
                if (!m.isSystem() && !m.senderUUID().equals(new UUID(0, 0)))
                    rememberPlayer(m.senderUUID(), m.rawPlayerName(), m.senderName().getString());
            }
        } else {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    try {
                        ChatMessage m = fromLine(line);
                        if (m == null || BlockList.isBlocked(m)) continue;
                        if (!skipCounts.isEmpty() && !takeFresh(skipCounts, mergeKeyOf(m))) continue;
                        messages.add(m);
                        if (!m.isSystem() && !m.senderUUID().equals(new UUID(0, 0)))
                            rememberPlayer(m.senderUUID(), m.rawPlayerName(), m.senderName().getString());
                    } catch (Exception e) {
                        E33Log.warn("[e33chat] Failed to read/write chat history", e);
                    }
                }
            } catch (Exception e) {
                E33Log.warn("[e33chat] Failed to read/write chat history", e);
            }
        }
        while (messages.size() > MAX) messages.remove(0);
        bumpListVersion();
    }

    private static final long SESSION_START_MS = System.currentTimeMillis();

    private static final java.util.regex.Pattern MERGE_NAME_COLOR =
        java.util.regex.Pattern.compile("§.");

    private static final Map<String, Integer> backlogKeys = new HashMap<>();

    private static String mergeKey(String senderName, String content, String group) {
        String name = senderName == null ? ""
            : MERGE_NAME_COLOR.matcher(senderName).replaceAll("").trim().toLowerCase(Locale.ROOT);
        return name + "\0" + (content == null ? "" : content)
            + "\0" + (group == null ? "" : group);
    }

    private static String mergeKeyOf(ChatMessage m) {
        String name = m.rawPlayerName() != null && !m.rawPlayerName().isEmpty()
            ? m.rawPlayerName()
            : (m.senderName() != null ? m.senderName().getString() : null);
        return mergeKey(name, m.content().getString(), m.group());
    }

    private static Map<String, Integer> mergeCountsOf(List<ChatMessage> rows) {
        Map<String, Integer> counts = new HashMap<>();
        for (ChatMessage m : rows)
            counts.merge(mergeKeyOf(m), Math.max(1, m.duplicateCount()), Integer::sum);
        return counts;
    }

    private static Map<String, Integer> usableSkips(Map<String, Integer> recorded,
                                                    Map<String, Integer> present) {
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, Integer> e : recorded.entrySet()) {
            Integer have = present.get(e.getKey());
            if (have == null || have <= 0) continue;
            out.put(e.getKey(), Math.min(e.getValue(), have));
        }
        return out;
    }

    private static boolean takeFresh(Map<String, Integer> counts, String key) {
        Integer left = counts.get(key);
        if (left != null && left > 0) {
            counts.put(key, left - 1);
            return false;
        }
        return true;
    }

    public static void addHistoryMessages(List<com.niuqu.chatbubble.network.HistoryPayload.HistoryEntry> entries) {
        if (entries == null || entries.isEmpty()) return;

        Map<String, Integer> seen = mergeCountsOf(messages);
        List<ChatMessage> fresh = new ArrayList<>();
        for (var e : entries) {
            if (e == null) continue;
            String sender = e.senderName() != null ? e.senderName() : "";
            String content = e.content() != null ? e.content() : "";
            if (content.isBlank()) continue;
            if (BlockList.isPlayerBlocked(sender, Text.literal(sender),
                ChatBubbleClientSetup.config().blockedPlayers())) continue;
            String key = mergeKey(sender, content, e.group());
            if (!takeFresh(seen, key)) continue;
            backlogKeys.merge(key, 1, Integer::sum);
            fresh.add(new ChatMessage(
                e.senderUUID() != null ? e.senderUUID() : new UUID(0, 0),
                Text.literal(sender),
                Text.literal(content),
                e.time(),
                false,
                e.isSystem(),
                e.replyContent(),
                e.replySender(),
                String.valueOf(content.hashCode()),
                1,
                sender,
                false,
                null,
                e.group()
            ));
            if (!e.isSystem() && e.senderUUID() != null && !e.senderUUID().equals(new UUID(0, 0)))
                rememberPlayer(e.senderUUID(), sender, sender);
        }
        if (fresh.isEmpty()) return;
        int at = 0;
        while (at < messages.size() && messages.get(at).time() < SESSION_START_MS) at++;
        messages.addAll(at, fresh);
        while (messages.size() > MAX) messages.remove(0);
        bumpListVersion();
    }

    public static void applyChatMeta(UUID senderUUID, String senderName, String messageHash,
                                      String quoteSender, String quoteContent, List<String> mentionTargets) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);

            boolean nameMatch = senderName != null && !senderName.isEmpty()
                && msg.rawPlayerName() != null && msg.rawPlayerName().equals(senderName);
            if (msg.messageHash().equals(messageHash)
                && (msg.senderUUID().equals(senderUUID) || nameMatch)) {
                if (msg.replyContent() != null) continue;

                if (msg.duplicateCount() > 1) continue;
                if (System.currentTimeMillis() - msg.time() > 5_000) continue;
                if (!quoteContent.isEmpty()) {
                    messages.set(i, msg.withReply(quoteContent, quoteSender));
                    bumpListVersion();
                    String playerName = localPlayerSupplier.get() != null
                        ? localPlayerSupplier.get().getName().getString() : "";
                    if (!msg.isOwn() && !playerName.isEmpty()
                        && playerName.equals(quoteSender)
                        && !msg.content().getString().contains("@" + playerName)
                        && ChatBubbleClientSetup.config().mentionSoundEnabled()) {
                        effectObserver.onQuoteSound();
                    }
                }
                return;
            }
        }
        if (!quoteContent.isEmpty()) {

            long cutoff = System.currentTimeMillis() - 10_000;
            EchoTracker.prunePendingMetas(cutoff);
            EchoTracker.putPendingMeta(messageHash, senderUUID, quoteSender, quoteContent, mentionTargets);
        }
    }
}
