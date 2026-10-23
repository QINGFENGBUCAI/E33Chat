package com.niuqu.chatbubble;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LangFileTest {

    @Test
    void zhAndEnHaveSameKeys() throws Exception {
        Set<String> zh = keys("/assets/e33chat/lang/zh_cn.json");
        Set<String> en = keys("/assets/e33chat/lang/en_us.json");
        assertFalse(zh.isEmpty(), "zh_cn.json has no keys");
        assertEquals(zh, en, "zh_cn / en_us key sets differ");
    }

    private static Set<String> keys(String resource) throws Exception {
        try (InputStream in = LangFileTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            JsonObject object = JsonParser
                .parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();
            return new TreeSet<>(object.keySet());
        }
    }
}
