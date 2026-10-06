package com.niuqu.chatbubble.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapped tooltip lines across the 1.21.6 GUI rework.
 *
 * {@code DrawContext.drawTooltip} took {@code List<OrderedText>} (produced by
 * {@code TextRenderer.wrapLines}) before 1.21.6; from 1.21.6 the wrapped overload
 * takes {@code List<Text>} instead, so the pre-wrapped lines are rebuilt as Texts
 * here. Without this the single-Text overload would render one unwrapped line and
 * long descriptions would overflow the screen.
 */
public final class TooltipCompat {
    private TooltipCompat() {}

    public static List<Text> wrap(TextRenderer font, Text text, int width) {
        List<Text> out = new ArrayList<>();
        if (text == null) return out;
        net.minecraft.text.Style style = text.getStyle();
        StringBuilder line = new StringBuilder();
        for (String word : text.getString().split(" ")) {
            if (line.length() == 0) {
                line.append(word);
                continue;
            }
            String candidate = line + " " + word;
            if (font.getWidth(candidate) > width) {
                out.add(Text.literal(line.toString()).setStyle(style));
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) out.add(Text.literal(line.toString()).setStyle(style));
        if (out.isEmpty()) out.add(Text.literal(text.getString()).setStyle(style));
        return out;
    }
}
