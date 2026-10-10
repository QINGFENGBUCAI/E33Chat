package com.niuqu.chatbubble.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

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
