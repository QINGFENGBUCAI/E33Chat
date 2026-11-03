package com.niuqu.chatbubble.chat.capture;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatPipelineNameTest {
    @Test void trailingAngleBracketTrimmed() {
        String[] lines = {
            "[Astrstag head]Astrstag> 你哈",
            "❤ MiTao_G> 你哈",
            "Steve>>> 你哈"
        };
        String[] expected = {
            "[Astrstag head]Astrstag",
            "❤ MiTao_G",
            "Steve"
        };
        for (int i = 0; i < lines.length; i++) {
            Text line = Text.literal(lines[i]);
            int idx = line.getString().lastIndexOf("你哈");
            assertEquals(expected[i],
                ChatPipeline.cleanNameArea(line, 0, idx, null, Text.literal("fallback")).getString());
        }
    }

    @Test void balancedAngleBracketsKept() {
        Text line = Text.literal("<Astrstag> 你哈");
        int idx = line.getString().lastIndexOf("你哈");
        assertEquals("<Astrstag>",
            ChatPipeline.cleanNameArea(line, 0, idx, null, Text.literal("fallback")).getString());
    }

    @Test void titledNameWithAngleBracketKept() {
        Text line = Text.literal("[称号]<Astrstag> 你哈");
        int idx = line.getString().lastIndexOf("你哈");
        assertEquals("[称号]Astrstag",
            ChatPipeline.cleanNameArea(line, 0, idx, "Astrstag", Text.literal("fallback")).getString());
    }

    @Test void separatorStillTrimmed() {
        String[] lines = {"[Astrstag head]Astrstag» 你哈", "Astrstag: 你哈", "Astrstag： 你哈"};
        String[] expected = {"[Astrstag head]Astrstag", "Astrstag", "Astrstag"};
        for (int i = 0; i < lines.length; i++) {
            Text line = Text.literal(lines[i]);
            int idx = line.getString().lastIndexOf("你哈");
            assertEquals(expected[i],
                ChatPipeline.cleanNameArea(line, 0, idx, null, Text.literal("fallback")).getString());
        }
    }

    @Test void extractDecoratedNameDropsTrailingAngleBracket() {
        Text line = Text.literal("[Astrstag head]Astrstag> 你哈");
        assertEquals("[Astrstag head]Astrstag",
            ChatPipeline.extractDecoratedName(line, "你哈", "Astrstag", Text.literal("Astrstag")).getString());
    }

    @Test void emptyNameAreaFallsBack() {
        Text line = Text.literal("> 你哈");
        int idx = line.getString().lastIndexOf("你哈");
        assertEquals("fallback",
            ChatPipeline.cleanNameArea(line, 0, idx, null, Text.literal("fallback")).getString());
    }
}
