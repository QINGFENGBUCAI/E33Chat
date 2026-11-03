package com.niuqu.chatbubble.chat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

// 消息展示解析
public final class MessagePresentation {
    // 私有构造
    private MessagePresentation() {}

    public record PlayerLine(String playerName, String displayLabel, String content,
                             int nameStart, int nameEnd, int contentStart) {}

    // 解析玩家发言行
    public static Optional<PlayerLine> parseDecoratedPlayerLine(
        String text, Collection<String> onlineNames
    ) {
        if (text == null || onlineNames == null) return Optional.empty();
        return onlineNames.stream()
            .filter(n -> n != null && !n.isBlank())
            .sorted(Comparator.comparingInt(String::length).reversed())
            .flatMap(name -> parseGeneric(text, name).stream())
            .findFirst();
    }

    // 通用玩家行解析
    static Optional<PlayerLine> parseGeneric(String text, String name) {
        if (text == null || name == null) return Optional.empty();
        String cleanName = name.replaceAll("§.", "");
        if (cleanName.isEmpty()) return Optional.empty();
        int[] map = new int[text.length()];
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '§' && i + 1 < text.length()) { i++; continue; }
            map[clean.length()] = i;
            clean.append(ch);
        }
        String ct = clean.toString();
        int idx = findNameIndex(ct, cleanName);
        if (idx < 0) return Optional.empty();
        String beforeName = ct.substring(0, idx);
        if (hasBareChatSeparator(beforeName)) return Optional.empty();
        if (isBroadcastLabelPrefix(ct, idx)) return Optional.empty();
        int minLen = 3;
        if (idx > 0 && ct.charAt(idx - 1) == '<') {
            int closeAngle = ct.indexOf('>', idx + cleanName.length());
            if (closeAngle >= 0 && closeAngle - (idx - 1) <= 64) minLen = 1;
        }
        if (minLen == 3 && idx > 0) {
            int bracketClose = ct.lastIndexOf(']', idx);
            if (bracketClose >= 0 && idx - bracketClose <= 2) {
                int bracketOpen = ct.lastIndexOf('[', bracketClose);
                if (bracketOpen >= 0) {
                    int after = idx + cleanName.length();
                    if (after < ct.length()) {
                        char next = ct.charAt(after);
                        if (next == ':' || next == '：') minLen = 1;
                    }
                }
            }
        }
        if (minLen == 3) {
            int after = idx + cleanName.length();
            if (after < ct.length()) {
                char next = ct.charAt(after);
                if (next == ':' || next == '：') minLen = 1;
            }
        }
        if (cleanName.length() < minLen) return Optional.empty();
        int decorativeLen = countDecorativePrefix(ct, idx);
        if (idx - decorativeLen >= 30) return Optional.empty();
        if (idx > 0) {
            char prev = ct.charAt(idx - 1);
            boolean prevIsColorCode = prev == '§' || (idx >= 2 && ct.charAt(idx - 2) == '§');
            if (!prevIsColorCode && (Character.isLetterOrDigit(prev) || prev == '_')) {
                int openAngle = ct.lastIndexOf('<', idx);
                int closeAngle = ct.indexOf('>', idx + cleanName.length());
                if (openAngle >= 0 && closeAngle >= 0 && closeAngle - openAngle <= 64) {
                } else {
                    int bracketClose = ct.lastIndexOf(']', idx);
                    if (bracketClose >= 0) {
                        int bracketOpen = ct.lastIndexOf('[', bracketClose);
                        if (bracketOpen < 0 || idx - bracketClose > 2) return Optional.empty();
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }
        int after = idx + cleanName.length();
        if (after < ct.length()) {
            char next = ct.charAt(after);
            if (Character.isLetterOrDigit(next) || next == '_') return Optional.empty();
        }
        int sep = skipSeparators(ct, after);
        if (sep <= after || sep >= ct.length()) return Optional.empty();
        int origNameStart = map[idx];
        int origNameEnd = map[idx + cleanName.length()];
        int origContentStart = map[sep];
        String displayLabel = text.substring(0, origNameEnd);
        return Optional.of(new PlayerLine(name, displayLabel, text.substring(origContentStart).strip(),
            origNameStart, origNameEnd, origContentStart));
    }

    // 查找名字位置
    private static int findNameIndex(String text, String name) {
        int from = 0;
        while (true) {
            int idx = text.indexOf(name, from);
            if (idx < 0) return -1;
            if (!insideDecoratedBracket(text, idx, name.length())) return idx;
            from = idx + 1;
        }
    }

    // 判断括号内名字
    private static boolean insideDecoratedBracket(String text, int idx, int nameLen) {
        int open = text.lastIndexOf('[', idx);
        if (open < 0) return false;
        int close = text.indexOf(']', idx + nameLen);
        if (close < 0 || close - open > 64) return false;
        if (text.indexOf(']', open) < idx) return false;
        String inner = text.substring(open + 1, close).trim();
        return !inner.equals(text.substring(idx, idx + nameLen));
    }

    // 跳过分隔符
    public static int skipSeparators(String text, int from) {
        int sep = from;
        while (sep < text.length()) {
            char ch = text.charAt(sep);
            if (ch == '§' && sep + 1 < text.length()) { sep += 2; continue; }
            if (ch == '[' || ch == '(' || ch == '<' || ch == '【') {
                char close = ch == '[' ? ']' : ch == '(' ? ')' : ch == '<' ? '>' : '】';
                int end = text.indexOf(close, sep + 1);
                if (end > sep && end - sep <= 32) { sep = end + 1; continue; }
            }
            if (Character.isWhitespace(ch) || ch == '>' || ch == ':'
                || ch == '：' || ch == '»' || ch == '-' || ch == '|') sep++;
            else break;
        }
        return sep;
    }

    // 冒号前含私聊词
    public static boolean hasWhisperKeywordBeforeColon(String text) {
        if (text == null) return false;
        int colon = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ':' || ch == '：') { colon = i; break; }
        }
        String zone = colon < 0 ? text : text.substring(0, colon);
        String lower = zone.toLowerCase();
        if (WhisperSignal.containsZh(zone))
            return true;
        String zoneNoBrackets = zone.replaceAll("\\[[^\\]]*\\]|\\([^\\)]*\\)", "");
        if (!WhisperSignal.EN.matcher(zoneNoBrackets.toLowerCase()).find()) return false;
        String rest = WhisperSignal.EN.matcher(zoneNoBrackets.toLowerCase()).replaceAll(" ").trim();
        return !rest.isEmpty();
    }

    // 提取私聊正文
    public static String extractWhisperContent(String fullText, String senderName) {
        if (senderName == null || senderName.isEmpty()) return fullText;
        int idx = fullText == null ? -1 : fullText.indexOf(senderName);
        if (idx < 0) return fullText;
        String after = fullText.substring(idx + senderName.length());
        for (String sep : new String[]{": ", "：", " :", " ：", " -> ", " >> ", " » ", " | "}) {
            int i = after.indexOf(sep);
            if (i >= 0) return after.substring(i + sep.length());
        }
        return after.trim();
    }

    // 解析发出的私聊对象
    public static String outgoingWhisperPartner(String text) {
        if (text == null) return null;
        String t = text.replaceAll("§.", "").trim();
        int i = 0;
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == '[' || c == '【') {
                char close = c == '[' ? ']' : '】';
                int end = t.indexOf(close, i + 1);
                if (end < 0 || end - i > 64) return null;
                i = end + 1;
                while (i < t.length() && Character.isWhitespace(t.charAt(i))) i++;
                continue;
            }
            break;
        }
        String rest = t.substring(i);
        String lower = rest.toLowerCase(java.util.Locale.ROOT);
        int nameStart = -1;
        for (String p : new String[]{"你悄悄地对", "你悄悄的对"}) {
            if (rest.startsWith(p)) { nameStart = p.length(); break; }
        }
        if (nameStart < 0 && lower.startsWith("you whisper to ")) {
            nameStart = "you whisper to ".length();
        }
        if (nameStart < 0) return null;
        int colon = -1;
        for (int j = nameStart; j < rest.length(); j++) {
            char ch = rest.charAt(j);
            if (ch == ':' || ch == '：') { colon = j; break; }
        }
        if (colon < 0) return null;
        String partner = rest.substring(nameStart, colon).trim();
        if (partner.endsWith("说")) partner = partner.substring(0, partner.length() - 1).trim();
        partner = partner.replaceAll("\\[[^\\]]*\\]|【[^】]*】|\\([^\\)]*\\)", "").trim();
        return partner.isEmpty() ? null : partner;
    }

    // 判断私聊前缀装饰
    public static boolean isWhisperDecoration(String text) {
        if (text == null) return false;
        String t = text.replaceAll("§.", "").trim();
        if (t.isEmpty()) return false;
        String lower = t.toLowerCase(java.util.Locale.ROOT);
        return t.contains("悄悄") || t.contains("对你说") || t.contains("私聊")
            || lower.contains("whisper to") || lower.contains("whispers to you")
            || lower.contains("whispering to");
    }

    // 去色索引映射回原文
    public static int[] cleanRangeToRaw(String raw, int cleanStart, int cleanLen) {
        if (raw == null || cleanStart < 0 || cleanLen <= 0) return null;
        int ci = 0, ri = 0, start = -1, end = -1;
        while (ri < raw.length()) {
            char ch = raw.charAt(ri);
            if (ch == '§' && ri + 1 < raw.length()) { ri += 2; continue; }
            if (ci == cleanStart) start = ri;
            ci++;
            ri++;
            if (ci == cleanStart + cleanLen) { end = ri; break; }
        }
        return start >= 0 && end > start ? new int[]{start, end} : null;
    }

    // 判断纯空白间隔
    public static boolean isWhitespaceOnlyGap(String text, int from, int to) {
        if (text == null || to <= from) return false;
        for (int i = from; i < to && i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) return false;
        }
        return true;
    }

    private static final java.util.Set<String> BROADCAST_LABELS = java.util.Set.of(
        "系统", "公告", "服务器", "广播", "提示", "通知",
        "system", "server", "notice", "broadcast", "announcement", "alert");

    // 检测裸聊天分隔符
    private static boolean hasBareChatSeparator(String text) {
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < n) { i += 2; continue; }
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '[' || c == '(' || c == '<' || c == '【') {
                char close = c == '[' ? ']' : c == '(' ? ')' : c == '<' ? '>' : '】';
                int j = text.indexOf(close, i + 1);
                if (j > i) {
                    String inner = text.substring(i + 1, j);
                    for (String token : inner.split("[|:：>»]")) {
                        if (!token.isBlank()
                            && BROADCAST_LABELS.contains(token.trim().toLowerCase(java.util.Locale.ROOT)))
                            return true;
                    }
                    i = j + 1;
                    continue;
                }
            }
            if (c == '>' || c == '»' || c == '|' || c == ':' || c == '：') return true;
            i++;
        }
        return false;
    }

    // 判断广播标签前缀
    static boolean isBroadcastLabelPrefix(String cleanText, int nameIdx) {
        String zone = cleanText.substring(0, nameIdx).trim();
        if (zone.isEmpty()) return false;
        while (zone.length() >= 2) {
            char open = zone.charAt(0);
            char close = zone.charAt(zone.length() - 1);
            if ((open == '[' && close == ']') || (open == '【' && close == '】')
                || (open == '<' && close == '>') || (open == '(' && close == ')')) {
                zone = zone.substring(1, zone.length() - 1).trim();
            } else {
                break;
            }
        }
        if (zone.isEmpty()) return false;
        return BROADCAST_LABELS.contains(zone.toLowerCase(java.util.Locale.ROOT));
    }

    // 统计装饰前缀长度
    private static int countDecorativePrefix(String text, int upTo) {
        int i = 0;
        while (i < upTo) {
            char c = text.charAt(i);
            if (c == '[') {
                int close = text.indexOf(']', i + 1);
                if (close >= 0 && close < upTo) { i = close + 1; continue; }
            }
            if (c == '<') {
                int close = text.indexOf('>', i + 1);
                if (close >= 0 && close < upTo) { i = close + 1; continue; }
            }
            if (c == '§' && i + 1 < upTo) { i += 2; continue; }
            if (Character.isWhitespace(c) || !Character.isLetterOrDigit(c)) { i++; continue; }
            break;
        }
        return i;
    }
}
