package com.niuqu.chatbubble.ui;

import com.niuqu.chatbubble.E33Log;
import com.niuqu.chatbubble.compat.TextureCompat;
import com.niuqu.chatbubble.image.RasterImageDecoder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EmoteStore {
    public static final int EMOTE_MAX = 32;
    private static final List<File> emotes = new ArrayList<>();
    private static final Map<File, Identifier> textures = new HashMap<>();
    private static int textureSeq;
    private static boolean scanned;

    private EmoteStore() {}

    public static File dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("e33chat/emotes").toFile();
    }

    public static boolean isImageFile(File f) {
        if (f == null) return false;
        String n = f.getName().toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
            || n.endsWith(".gif");
    }

    private static boolean isImage(File f) { return isImageFile(f); }

    public static void refresh() {
        scanned = true;
        emotes.clear();
        File d = dir();
        File[] files = d.listFiles();
        if (files != null) {
            for (File f : files) {
                if (isImage(f)) emotes.add(f);
            }
            emotes.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            while (emotes.size() > EMOTE_MAX) emotes.remove(emotes.size() - 1);
        }
        textures.keySet().removeIf(f -> !emotes.contains(f));
    }

    public static List<File> list() {
        if (!scanned) refresh();
        return emotes;
    }

    public static boolean add(File f) {
        if (!isImage(f)) return false;
        if (emotes.size() >= EMOTE_MAX) return false;
        try {
            File d = dir();
            if (!d.isDirectory() && !d.mkdirs()) return false;
            File dest = new File(d, f.getName());
            Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            refresh();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean remove(File f) {
        if (f.delete()) {
            refresh();
            return true;
        }
        return false;
    }

    public static boolean addBytes(byte[] png, String name) {
        if (png == null || png.length == 0) return false;
        if (emotes.size() >= EMOTE_MAX) return false;
        try {
            File d = dir();
            if (!d.isDirectory() && !d.mkdirs()) return false;
            String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!safe.endsWith(".png")) safe += ".png";
            File dest = new File(d, safe);
            Files.write(dest.toPath(), png);
            refresh();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isFull() {
        return emotes.size() >= EMOTE_MAX;
    }

    public static Identifier texture(File f) {
        Identifier id = textures.get(f);
        if (id != null) return id;
        try {
            RasterImageDecoder.DecodedImage dec =
                RasterImageDecoder.decode(Files.readAllBytes(f.toPath()));
            if (dec == null) {
                E33Log.warn("[e33chat] emote decode failed: {}", f.getName());
                return null;
            }

            //#if MC >= 12104
            Identifier tex = Identifier.of("e33chat", "emote_" + (textureSeq++));
            MinecraftClient.getInstance().getTextureManager()
                .registerTexture(tex, TextureCompat.create(tex.getPath(), dec.image()));
            //#else
            //$$ Identifier tex = MinecraftClient.getInstance().getTextureManager()
            //$$     .registerDynamicTexture("e33chat_emote_" + (textureSeq++),
            //$$         TextureCompat.create("e33chat_emote", dec.image()));
            //#endif
            textures.put(f, tex);
            return tex;
        } catch (IOException e) {
            E33Log.warn("[e33chat] emote read failed: {}", f.getName(), e);
            return null;
        }
    }
}
