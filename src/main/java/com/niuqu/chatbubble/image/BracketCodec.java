package com.niuqu.chatbubble.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BracketCodec {
    private static final Pattern BRACKET = Pattern.compile(
        "\\[\\[(ChatUpgrade|CICode|E33Emote),([^\\]]+)\\]\\]", Pattern.CASE_INSENSITIVE);

    public record ImageRef(String url, String name, boolean emote) {
        public ImageRef(String url, String name) { this(url, name, false); }
    }

    public record ParseResult(Text textWithoutImages, List<ImageRef> images) {}

    private BracketCodec() {}

    public static ParseResult parse(Text text) {
        if (text == null) return new ParseResult(text, List.of());
        String plain = text.getString();
        Matcher m = BRACKET.matcher(plain);
        if (!m.find()) return new ParseResult(text, List.of());

        List<ImageRef> images = new ArrayList<>();
        MutableText out = Text.empty();
        boolean[] hasText = {false};
        int[] segIndex = {0};

        text.visit((style, part) -> {
            int partStart = 0;
            Matcher local = BRACKET.matcher(part);
            while (local.find()) {
                if (local.start() > partStart) {
                    out.append(Text.literal(part.substring(partStart, local.start())).fillStyle(style));
                    hasText[0] = true;
                    segIndex[0]++;
                }
                ImageRef ref = parseAttrs(local.group(2), local.group(1));
                if (ref != null) images.add(ref);
                partStart = local.end();
            }
            if (partStart < part.length()) {
                out.append(Text.literal(part.substring(partStart)).fillStyle(style));
                hasText[0] = true;
                segIndex[0]++;
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (images.isEmpty() && !hasText[0]) {
            String stripped = m.replaceAll("");
            return new ParseResult(Text.literal(stripped).setStyle(text.getStyle()), List.of());
        }
        return new ParseResult(out, images);
    }

    public static ParseResult parseOrExtract(Text text) {
        ParseResult r = parse(text);
        if (!r.images().isEmpty() || text == null) return r;
        List<ImageRef> refs = extractFromHover(text);
        if (refs.isEmpty()) refs = extractFromShowTextHover(text);
        if (refs.isEmpty()) return r;
        MutableText out = Text.empty();
        text.visit((style, part) -> {
            net.minecraft.text.HoverEvent hover = style.getHoverEvent();
            if (hover != null && (isChatImageHover(hover) || isEasyBotCICodeHover(hover))) {
                return Optional.empty();
            }
            out.append(Text.literal(part).fillStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return new ParseResult(out, refs);
    }

    public static Text toPlaceholderText(Text text) {
        if (text == null) return null;
        Matcher m = BRACKET.matcher(text.getString());
        if (!m.find()) return text;
        MutableText out = Text.empty();
        Text placeholder = Text.translatable("e33chat.image.placeholder")
            .formatted(Formatting.GREEN);
        text.visit((style, part) -> {
            int partStart = 0;
            Matcher local = BRACKET.matcher(part);
            while (local.find()) {
                if (local.start() > partStart) {
                    out.append(Text.literal(part.substring(partStart, local.start())).fillStyle(style));
                }
                out.append(placeholder.copy().fillStyle(style));
                partStart = local.end();
            }
            if (partStart < part.length()) {
                out.append(Text.literal(part.substring(partStart)).fillStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static ImageRef parseAttrs(String attrs, String tag) {
        String url = null;
        String name = null;
        String type = null;
        for (String kv : attrs.split(",")) {
            int eq = kv.indexOf('=');
            if (eq <= 0) continue;
            String key = kv.substring(0, eq).trim().toLowerCase();
            String val = kv.substring(eq + 1).trim();
            if (val.isEmpty()) continue;
            switch (key) {
                case "url" -> url = val;
                case "name" -> name = val;
                case "type" -> type = val;
                default -> { }
            }
        }
        if (url == null || url.isBlank()) return null;

        if (type != null && !type.equalsIgnoreCase("image")) return null;

        return new ImageRef(url, name, tag.equalsIgnoreCase("E33Emote"));
    }

    public static List<ImageRef> extractFromHover(Text text) {
        if (text == null) return List.of();
        List<ImageRef> out = new ArrayList<>();
        text.visit((style, part) -> {
            net.minecraft.text.HoverEvent hover = style.getHoverEvent();
            if (hover != null && isChatImageHover(hover)) {
                String url = readUrlFromHover(hover);
                if (url != null && !url.isBlank()) {
                    out.add(new ImageRef(url, null));
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    public static List<ImageRef> extractFromShowTextHover(Text text) {
        if (text == null) return List.of();
        List<ImageRef> out = new ArrayList<>();
        text.visit((style, part) -> {
            net.minecraft.text.HoverEvent hover = style.getHoverEvent();
            if (hover != null && isEasyBotCICodeHover(hover)) {
                String tooltip = hoverText(hover);
                if (tooltip != null) {
                    Matcher m = BRACKET.matcher(tooltip);
                    while (m.find()) {
                        ImageRef ref = parseAttrs(m.group(2), m.group(1));
                        if (ref != null) out.add(ref);
                    }
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static boolean isEasyBotCICodeHover(net.minecraft.text.HoverEvent hover) {
        try {
            if (hover.getAction() != net.minecraft.text.HoverEvent.Action.SHOW_TEXT) return false;
            String tooltip = hoverText(hover);
            return tooltip != null && BRACKET.matcher(tooltip).find();
        } catch (Throwable t) {
            return false;
        }
    }

    private static String hoverText(net.minecraft.text.HoverEvent hover) {
        try {
            Text show = com.niuqu.chatbubble.compat.StyleCompat.hoverShowText(hover);
            if (show != null) return show.getString();
            Object value = com.niuqu.chatbubble.compat.StyleCompat.hoverValue(hover);
            if (value instanceof Text c) return c.getString();
            if (value instanceof String s) return s;
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    private static boolean isChatImageHover(net.minecraft.text.HoverEvent hover) {
        try {
            if (String.valueOf(hover.getAction()).toLowerCase(java.util.Locale.ROOT).contains("chatimage")) return true;

            Object value = com.niuqu.chatbubble.compat.StyleCompat.hoverValue(hover);
            return value != null
                && value.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("chatimage");
        } catch (Throwable t) {
            return false;
        }
    }

    private static String readUrlFromHover(net.minecraft.text.HoverEvent hover) {
        try {
            Object value = com.niuqu.chatbubble.compat.StyleCompat.hoverValue(hover);

            if (value instanceof com.google.gson.JsonElement je) {
                if (je.isJsonObject() && je.getAsJsonObject().has("url")
                        && je.getAsJsonObject().get("url").isJsonPrimitive()) {
                    return normalizeUrl(je.getAsJsonObject().get("url").getAsString());
                }
                if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isString()) {
                    return normalizeUrl(je.getAsString());
                }
            } else if (value instanceof String s) {
                return normalizeUrl(s);
            } else if (value != null) {
                return normalizeUrl(String.valueOf(value));
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    static String normalizeUrl(String s) {
        if (s == null || s.isBlank()) return null;
        String fromCode = urlFromCodeText(s);
        if (fromCode != null) return fromCode;
        String trimmed = s.trim();
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        return (lower.startsWith("http://") || lower.startsWith("https://")) ? trimmed : null;
    }

    static String urlFromCodeText(String s) {
        if (s == null || s.isEmpty()) return null;
        Matcher m = BRACKET.matcher(s);
        while (m.find()) {
            ImageRef ref = parseAttrs(m.group(2), m.group(1));
            if (ref != null) return ref.url();
        }
        return null;
    }
}
