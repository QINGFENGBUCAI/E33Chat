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

// 聊天消息存储
public class ChatMessageStore {
    // 消息效果观察者
    public interface MessageEffectObserver {
        // 提及引用回调
        void onMentionOrQuote(Text content, SenderMeta meta, int index, String replySender);
        // 私聊接收回调
        void onWhisperReceived(UUID senderUUID, Text senderName, Text content, int index);
        // 系统消息回调
        void onSystemMessage(Text content, int index);
        // 公开消息音效
        void onPublicChatSound();
        // 引用音效回调
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

    // 设置消息效果观察者
    public static void setMessageEffectObserver(MessageEffectObserver observer) {
        effectObserver = observer != null ? observer : NOOP_OBSERVER;
    }

    private static final int MAX = 10000;
    private static final List<ChatMessage> messages = new ArrayList<>();
    private static int unreadCount = 0;
    private static boolean screenOpen = false;
    private static String pendingReplyContent;
    private static String pendingReplySender;

    // 判断转发是否重复
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
    // 设置使用TPA
    public static void setServerUseTpa(boolean v) { serverUseTpa = v; }
    // 是否使用TPA
    public static boolean useTpa() { return serverUseTpa; }

    private static volatile List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverChatTemplates = List.of();
    private static volatile List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverWhisperTemplates = List.of();
    private static volatile boolean serverTemplateDebug = false;

    private static volatile boolean easyBotCompat = true;

    // 设置EasyBot兼容
    public static void setEasyBotCompat(boolean v) { easyBotCompat = v; }
    // 是否EasyBot兼容
    public static boolean isEasyBotCompat() { return easyBotCompat; }

    // 设置服务端配置
    public static void setServerConfig(boolean useTpa, List<String> chatTemplates,
                                       List<String> whisperTemplates, boolean templateDebug) {
        serverUseTpa = useTpa;
        serverChatTemplates = compileTemplates(chatTemplates);
        serverWhisperTemplates = compileTemplates(whisperTemplates);
        serverTemplateDebug = templateDebug;
    }

    // 编译服务端模板
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

    // 获取聊天模板
    public static List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverChatTemplates() {
        return serverChatTemplates;
    }

    // 获取私聊模板
    public static List<com.niuqu.chatbubble.chat.TemplateMatcher.CompiledTemplate> serverWhisperTemplates() {
        return serverWhisperTemplates;
    }

    // 是否模板调试
    public static boolean serverTemplateDebug() { return serverTemplateDebug; }

    // 记录玩家名称
    public static void rememberPlayer(UUID uuid, String profileName, String displayName) {
        if (uuid == null || uuid.equals(new UUID(0, 0)) || profileName == null || profileName.isEmpty()) return;
        SeenPlayer existing = seenPlayers.get(uuid);
        String newDisplay = (displayName != null && !displayName.isEmpty()) ? displayName
            : (existing != null ? existing.displayName() : null);
        seenPlayers.put(uuid, new SeenPlayer(uuid, profileName, newDisplay));
    }

    // 获取已知名称变体
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

    // 按名称查找UUID
    public static UUID findSeenUuid(String name) {
        if (name == null || name.isEmpty()) return null;
        String stripped = name.replaceAll("§.", "");
        for (SeenPlayer sp : seenPlayers.values()) {
            if (matchesSeenName(name, stripped, sp.profileName())) return sp.uuid();
            if (matchesSeenName(name, stripped, sp.displayName())) return sp.uuid();
        }
        return null;
    }

    // 匹配已记录名称
    private static boolean matchesSeenName(String raw, String stripped, String stored) {
        if (stored == null || stored.isEmpty()) return false;
        if (raw.equals(stored)) return true;
        if (!stripped.isEmpty() && stripped.equals(stored)) return true;
        String storedStripped = stored.replaceAll("§.", "");
        return raw.equals(storedStripped) || (!stripped.isEmpty() && stripped.equals(storedStripped));
    }

    // 判断是否为已知玩家
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

    // 设置待处理元数据
    public static void setPendingMeta(SenderMeta meta) {
        EchoTracker.setPendingMeta(meta);
    }

    // 取出待处理元数据
    public static SenderMeta consumePendingMeta() {
        return EchoTracker.consumePendingMeta();
    }

    // 标记私聊回声
    public static void markPendingWhisperEcho(String target) {
        EchoTracker.markPendingWhisperEcho(target);
    }

    // 标记抑制捕获
    public static void markSuppressCapture() {
        EchoTracker.markSuppressCapture();
    }

    // 取出抑制引用标记
    public static boolean consumeSuppressQuoted() {
        return EchoTracker.consumeSuppressQuoted();
    }

    // 清理过期私聊回声
    private static void purgeStaleWhisperEchoes() {
        EchoTracker.purgeStaleWhisperEchoes();
    }

    // 是否存在私聊回声
    public static boolean hasPendingWhisperEcho() {
        return EchoTracker.hasPendingWhisperEcho();
    }

    // 获取私聊回声目标
    public static String getPendingWhisperTarget() {
        return EchoTracker.getPendingWhisperTarget();
    }

    // 消耗私聊回声
    public static void consumeWhisperEcho() {
        EchoTracker.consumeWhisperEcho();
    }

    // 取出抑制捕获标记
    public static boolean consumeSuppressCapture() {
        return EchoTracker.consumeSuppressCapture();
    }

    // 清理过期回声
    private static void purgeStaleEchoes() {
        EchoTracker.purgeStaleEchoes();
    }

    // 记录待匹配回声
    public static void incrementPendingEcho(String sentText) {
        EchoTracker.incrementPendingEcho(sentText);
    }

    // 按系统聊天匹配回声
    public static EchoTracker.EchoMatch consumeEchoBySystemChat(String incomingText) {
        return EchoTracker.consumeEchoBySystemChat(incomingText);
    }

    // 调试日志输出
    public static void debugLog(java.util.function.Supplier<String> msg) {
        if (ChatBubbleClientSetup.config().debugLog())
            E33Log.info(msg.get());
    }

    // 调试日志输出
    public static void debugLog(String msg) {
        debugLog(() -> msg);
    }

    // 按发送者匹配回声
    public static EchoTracker.EchoMatch consumeEchoIfSenderMatches(UUID senderUUID, Text senderName, String incomingText) {
        return EchoTracker.consumeEchoIfSenderMatches(senderUUID, senderName, incomingText);
    }

    // 判断包含完整名称
    public static boolean containsWholeName(String haystack, String needle) {
        return EchoTracker.containsWholeName(haystack, needle);
    }

    // 判断名称字符
    public static boolean isNamePart(char c) {
        return EchoTracker.isNamePart(c);
    }

    // 更新己方发送者名
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

    // 判断近期重复消息
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
        // 替换发送者名称
        public ChatMessage withSenderName(Text newSenderName) {
            return new ChatMessage(senderUUID, newSenderName, content, time,
                isOwn, isSystem, replyContent, replySender, messageHash, duplicateCount,
                rawPlayerName, whisper, whisperPartner, group);
        }
        // 生成合并消息
        public ChatMessage withMerge(String newReplyContent, String newReplySender, long newTime, int newDupCount) {
            return new ChatMessage(senderUUID, senderName, content, newTime,
                isOwn, isSystem, newReplyContent, newReplySender, messageHash, newDupCount,
                rawPlayerName, whisper, whisperPartner, group);
        }
        // 生成带引用消息
        public ChatMessage withReply(String newReplyContent, String newReplySender) {
            return new ChatMessage(senderUUID, senderName, content, time,
                isOwn, isSystem, newReplyContent, newReplySender, messageHash, duplicateCount,
                rawPlayerName, whisper, whisperPartner, group);
        }
    }

    // 判断是否同一发送者
    private static boolean isSameSender(ChatMessage last, Text senderName, String rawPlayerName) {
        if (rawPlayerName != null && !rawPlayerName.isEmpty()
            && last.rawPlayerName() != null && !last.rawPlayerName().isEmpty()) {
            return rawPlayerName.equals(last.rawPlayerName());
        }
        return last.senderName().getString().equals(senderName.getString());
    }

    // 清除被屏蔽消息
    public static void purgeBlocked(List<? extends String> blocked) {
        if (blocked == null || blocked.isEmpty()) return;
        messages.removeIf(m -> !m.isOwn() && !m.isSystem()
            && BlockList.isPlayerBlocked(m.rawPlayerName(), m.senderName(), blocked));
        bumpListVersion();
    }

    public static java.util.function.Supplier<net.minecraft.entity.player.PlayerEntity> localPlayerSupplier =
        () -> net.minecraft.client.MinecraftClient.getInstance().player;

    // 添加聊天消息
    public static void addMessage(Text content, UUID senderUUID, Text senderName, boolean isSystem, String rawPlayerName, boolean whisper, String whisperPartner, boolean localSend) {
        addMessage(content, senderUUID, senderName, isSystem, rawPlayerName, whisper, whisperPartner, localSend, null);
    }

    // 添加群组消息
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

    // 添加聊天消息
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
        if (!isSystem && senderUUID != null && !senderUUID.equals(new UUID(0, 0)))
            com.niuqu.chatbubble.render.SkinResolver.enqueuePrewarm(senderUUID,
                rawPlayerName != null && !rawPlayerName.isEmpty() ? rawPlayerName : senderName.getString());
        while (messages.size() > MAX)
            messages.remove(0);
        bumpListVersion();
        historyDirty = true;
        boolean isMentionOrQuote = !isSystem
            && com.niuqu.chatbubble.chat.MentionDetector.isMentioned(
                content.getString(), playerName,
                ChatBubbleClientSetup.config().mentionRequireAt(), replySender);
        if (isMentionOrQuote) {
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

    // 截取带样式文本
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

    // 转为单行文本
    public static String singleLine(String s) {
        return stripControls(s);
    }

    // 移除控制字符
    private static String stripControls(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c < 0x20 || c == 0x7F ? ' ' : c);
        }
        return sb.toString();
    }

    // 获取全部消息
    public static List<ChatMessage> getMessages() {
        return messages;
    }

    private static int listVersion = 0;
    // 递增列表版本
    private static void bumpListVersion() { listVersion++; }

    private static int publicCacheVersion = -1;
    private static List<ChatMessage> publicCache = List.of();

    private static final Map<String, List<ChatMessage>> whisperCache = new HashMap<>();
    private static int whisperCacheVersion = -1;

    // 获取列表版本
    public static int listVersion() { return listVersion; }

    // 获取私聊消息
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

    // 获取公开消息
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

    // 获取最近私聊
    public static ChatMessage getLatestWhisperWith(String partnerName) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.whisper() && partnerName.equals(msg.whisperPartner())) {
                return msg;
            }
        }
        return null;
    }

    // 获取最近公开消息
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

    // 标记私聊未读
    public static void markWhisperUnread(String partner) {
        if (partner != null) unreadWhisperPartners.add(partner);
    }

    // 清除私聊未读
    public static void clearUnreadWhisper(String partner) {
        unreadWhisperPartners.remove(partner);
    }

    // 判断私聊未读
    public static boolean hasUnreadWhisper(String partner) {
        return unreadWhisperPartners.contains(partner);
    }

    // 获取未读数量
    public static int getUnreadCount() {
        return unreadCount;
    }

    // 设置界面打开状态
    public static void setScreenOpen(boolean open) {
        screenOpen = open;
        if (open) {
            unreadCount = 0;
        }
    }

    // 按索引获取消息
    public static ChatMessage getMessageAt(int index) {
        if (index < 0 || index >= messages.size()) return null;
        return messages.get(index);
    }

    // 设置待引用回复
    public static void setPendingReply(String content, String sender) {
        pendingReplyContent = content;
        pendingReplySender = sender;
        EchoTracker.markQuoteSent();
    }

    // 获取待回复发送者
    public static String getPendingReplySender() { return pendingReplySender; }

    // 生成时间分组键
    public static String timeKey(long timeMillis, int interval) {
        if (interval <= 0) return "";
        return String.valueOf(timeMillis / (interval * 60_000L));
    }

    // 格式化时间
    public static String formatTime(long timeMillis) {
        var dt = java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        java.time.LocalDate today = java.time.LocalDate.now();
        if (dt.toLocalDate().equals(today)) return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (dt.getYear() == today.getYear()) return dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // 判断引用是否近期
    public static boolean wasRecentQuoteAt(long quoteSendTime, long now) {
        return EchoTracker.wasRecentQuoteAt(quoteSendTime, now);
    }

    // 判断是否近期引用
    public static boolean wasRecentQuote() {
        return EchoTracker.wasRecentQuote();
    }

    // 提取私聊内容
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

    // 提取私聊显示名
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

    // 移除斜体样式
    private static Text stripItalic(Text src) {
        MutableText out = Text.empty();
        src.visit((style, text) -> {
            out.append(Text.literal(text).fillStyle(style.withItalic(false)));
            return java.util.Optional.<Object>empty();
        }, net.minecraft.text.Style.EMPTY);
        return out;
    }

    private static Text ownDecoratedName;

    // 获取己方显示名
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

    // 缓存己方装饰名
    public static void cacheOwnDecoratedName(Text senderName) {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        String bare = player != null ? player.getName().getString() : "";
        if (senderName == null || bare.isEmpty()) return;
        String sn = senderName.getString();
        if (!sn.isEmpty() && !sn.equals(bare)) ownDecoratedName = senderName;
    }

    // 获取消息数量
    public static int size() {
        return messages.size();
    }

    // 切换当前世界
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
        if (ChatBubbleClientSetup.config().chatHistoryEnabled() && isWorldSpecific(currentWorldKey))
            loadMessages(currentWorldKey, java.util.Collections.emptyMap());
    }

    // 判断是否具体世界
    private static boolean isWorldSpecific(String key) {
        return key != null && (key.startsWith("SP:") || key.startsWith("MP:"));
    }

    // 获取历史文件
    private static File getHistoryFile(String worldKey) {
        return HistoryStore.getHistoryFile(worldKey);
    }

    // 获取旧版历史文件
    private static File getLegacyHistoryFile(String worldKey) {
        return HistoryStore.getLegacyHistoryFile(worldKey);
    }

    // 计算短哈希
    private static String sha256Short(String s) {
        return HistoryStore.sha256Short(s);
    }

    // 判断敏感命令
    public static boolean isSensitiveCommand(String text) {
        return HistoryStore.isSensitiveCommand(text);
    }

    // 序列化消息行
    public static String toLine(ChatMessage msg) {
        return HistoryStore.toLine(msg);
    }

    // 反序列化消息行
    public static ChatMessage fromLine(String line) {
        return HistoryStore.fromLine(line);
    }

    // 解析JSON消息行
    private static ChatMessage fromJsonLine(String line) {
        return HistoryStore.fromJsonLine(line);
    }

    // 解析文本组件
    private static Text componentFrom(Map<String, Object> obj, String jsonKey, String textKey) {
        return HistoryStore.componentFrom(obj, jsonKey, textKey);
    }

    // 转义字段
    private static String escapeField(String s) {
        return HistoryStore.escapeField(s);
    }

    // 还原转义字段
    private static String unescapeField(String s) {
        return HistoryStore.unescapeField(s);
    }

    // 解析带样式文本
    public static Text parseStyledText(String s) {
        return HistoryStore.parseStyledText(s);
    }

    // 应用颜色代码
    private static Style applySectionCode(Style style, char code) {
        return HistoryStore.applySectionCode(style, code);
    }

    // 加载旧版历史
    private static List<ChatMessage> loadLegacyFile(File f) {
        return HistoryStore.loadLegacyFile(f);
    }

    private static final java.util.concurrent.ExecutorService SAVE_EXECUTOR =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "e33chat-history-save");
            t.setDaemon(true);
            return t;
        });

    // 保存消息历史
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

    // 是否有可清理历史
    public static boolean hasHistoryToClear() {
        if (!messages.isEmpty()) return true;
        if (currentWorldKey == null) return false;
        return getHistoryFile(currentWorldKey).exists()
            || getLegacyHistoryFile(currentWorldKey).exists();
    }

    // 清除当前世界历史
    public static void clearCurrentWorldHistory() {
        messages.clear();
        backlogKeys.clear();
        bumpListVersion();
        unreadCount = 0;
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

    // 判断历史是否过期
    public static boolean isExpired(long fileMtime, long now, int retentionDays) {
        return HistoryStore.isExpired(fileMtime, now, retentionDays);
    }

    // 清理过期历史
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

    // 必要时自动保存
    public static void maybeAutoSave() {
        long now = System.currentTimeMillis();
        if (currentWorldKey == null || !historyDirty || now - lastAutoSave < AUTO_SAVE_MS) return;
        historyDirty = false;
        lastAutoSave = now;
        saveMessages(currentWorldKey);
    }

    // 加载消息历史
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

    // 生成合并键
    private static String mergeKey(String senderName, String content, String group) {
        String name = senderName == null ? ""
            : MERGE_NAME_COLOR.matcher(senderName).replaceAll("").trim().toLowerCase(Locale.ROOT);
        return name + "\0" + (content == null ? "" : content)
            + "\0" + (group == null ? "" : group);
    }

    // 生成消息合并键
    private static String mergeKeyOf(ChatMessage m) {
        String name = m.rawPlayerName() != null && !m.rawPlayerName().isEmpty()
            ? m.rawPlayerName()
            : (m.senderName() != null ? m.senderName().getString() : null);
        return mergeKey(name, m.content().getString(), m.group());
    }

    // 统计合并计数
    private static Map<String, Integer> mergeCountsOf(List<ChatMessage> rows) {
        Map<String, Integer> counts = new HashMap<>();
        for (ChatMessage m : rows)
            counts.merge(mergeKeyOf(m), Math.max(1, m.duplicateCount()), Integer::sum);
        return counts;
    }

    // 计算可用跳过数
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

    // 取出一个未重复项
    private static boolean takeFresh(Map<String, Integer> counts, String key) {
        Integer left = counts.get(key);
        if (left != null && left > 0) {
            counts.put(key, left - 1);
            return false;
        }
        return true;
    }

    // 添加历史消息
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

    // 应用聊天引用元数据
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
