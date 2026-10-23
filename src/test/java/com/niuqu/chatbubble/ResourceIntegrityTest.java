package com.niuqu.chatbubble;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class ResourceIntegrityTest {

    private static final String[] MIXIN_SECTIONS = {"mixins", "client", "server", "common"};
    private static final Set<String> OPTIONAL_ENTRYPOINTS = Set.of("modmenu");

    @Test
    void entrypointsAndMixinsResolve() throws Exception {
        try (InputStream in = ResourceIntegrityTest.class.getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(in, "fabric.mod.json missing from runtime resources");
            JsonObject root = JsonParser
                .parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();

            JsonObject entrypoints = root.getAsJsonObject("entrypoints");
            for (String type : entrypoints.keySet()) {
                if (OPTIONAL_ENTRYPOINTS.contains(type)) {
                    continue;
                }
                for (JsonElement entry : entrypoints.getAsJsonArray(type)) {
                    assertClassLoadable(entry.getAsString());
                }
            }

            for (JsonElement mixinEntry : root.getAsJsonArray("mixins")) {
                String config = mixinEntry.getAsJsonObject().get("config").getAsString();
                try (InputStream mixinIn = ResourceIntegrityTest.class.getResourceAsStream("/" + config)) {
                    assertNotNull(mixinIn, "mixin config missing: " + config);
                    JsonObject mixin = JsonParser
                        .parseReader(new InputStreamReader(mixinIn, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                    String packageName = mixin.get("package").getAsString();
                    for (String section : MIXIN_SECTIONS) {
                        if (!mixin.has(section)) {
                            continue;
                        }
                        for (JsonElement mixinClass : mixin.getAsJsonArray(section)) {
                            assertClassLoadable(packageName + "." + mixinClass.getAsString());
                        }
                    }
                }
            }
        }
    }

    private static void assertClassLoadable(String name) {
        try {
            Class.forName(name, false, ResourceIntegrityTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            fail("class referenced by resources not found: " + name, e);
        }
    }
}
