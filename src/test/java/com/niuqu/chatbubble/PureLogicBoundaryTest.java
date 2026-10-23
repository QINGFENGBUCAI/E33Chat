package com.niuqu.chatbubble;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PureLogicBoundaryTest {

    private static final List<String> PURE_FILES = List.of(
        "src/main/java/com/niuqu/chatbubble/config/ChatBubbleConfig.java",
        "src/main/java/com/niuqu/chatbubble/config/ConfigManager.java",
        "src/main/java/com/niuqu/chatbubble/config/ServerConfig.java",
        "src/main/java/com/niuqu/chatbubble/config/ServerConfigManager.java");

    @Test
    void pureLogicClassesDoNotImportMinecraft() throws IOException {
        Path root = ProjectFiles.root();
        StringBuilder violations = new StringBuilder();
        for (String relative : PURE_FILES) {
            Path file = root.resolve(relative);
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import net.minecraft")) {
                    violations.append(relative).append(": ").append(trimmed).append('\n');
                }
            }
        }
        assertFalse(violations.length() > 0, "pure logic classes must not import Minecraft:\n" + violations);
    }
}
