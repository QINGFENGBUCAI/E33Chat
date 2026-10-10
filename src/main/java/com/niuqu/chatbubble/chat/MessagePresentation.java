package com.niuqu.chatbubble.chat;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public final class MessagePresentation {
    private MessagePresentation() {}

    public record PlayerLine(String playerName, String displayLabel, String content,
                             int nameStart, int nameEnd, int contentStart) {}

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
        int idx = ct.indexOf(cleanName);
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
