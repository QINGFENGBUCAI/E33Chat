package com.niuqu.chatbubble;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreprocessorDirectiveTest {

    private static final Pattern IF_DIRECTIVE = Pattern.compile("^\\s*//#if\\b(.*)$");
    private static final Pattern ENDIF_DIRECTIVE = Pattern.compile("^\\s*//#endif\\b.*$");
    private static final Pattern CONDITION = Pattern.compile("^\\s+MC\\s*(>=|<=|==|>|<)\\s*(\\d{5}|\\d{6})\\s*$");

    @Test
    void directivesAreBalancedAndWellFormed() throws IOException {
        Path sourceRoot = ProjectFiles.root().resolve("src/main/java");
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                checkFile(file, problems);
            }
        }
        assertTrue(problems.isEmpty(), "preprocessor directive problems:\n" + String.join("\n", problems));
    }

    private static void checkFile(Path file, List<String> problems) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int depth = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (IF_DIRECTIVE.matcher(line).matches()) {
                depth++;
                String condition = line.substring(line.indexOf("#if") + 3);
                if (!CONDITION.matcher(condition).matches()) {
                    problems.add(file + ":" + (i + 1) + " invalid condition: " + line.trim());
                }
                if (condition.contains("&&") || condition.contains("||")) {
                    problems.add(file + ":" + (i + 1) + " compound condition: " + line.trim());
                }
            } else if (ENDIF_DIRECTIVE.matcher(line).matches()) {
                depth--;
                if (depth < 0) {
                    problems.add(file + ":" + (i + 1) + " unbalanced #endif");
                    depth = 0;
                }
            }
        }
        if (depth != 0) {
            problems.add(file + " unbalanced #if depth=" + depth);
        }
        long ifCount = lines.stream().filter(IF_DIRECTIVE.asPredicate()).count();
        long endifCount = lines.stream().filter(ENDIF_DIRECTIVE.asPredicate()).count();
        assertEquals(ifCount, endifCount, file + " #if/#endif count mismatch");
    }
}
