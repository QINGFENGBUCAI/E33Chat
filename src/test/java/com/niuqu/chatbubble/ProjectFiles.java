package com.niuqu.chatbubble;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class ProjectFiles {

    private ProjectFiles() {
    }

    static Path root() {
        Path path = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (path != null) {
            if (Files.exists(path.resolve("settings.json")) && Files.isDirectory(path.resolve("versions"))) {
                return path;
            }
            path = path.getParent();
        }
        throw new IllegalStateException("project root not found from " + System.getProperty("user.dir"));
    }
}
