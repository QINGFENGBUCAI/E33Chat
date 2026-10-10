package com.niuqu.chatbubble.render;

import com.niuqu.chatbubble.E33Log;
import com.niuqu.chatbubble.compat.TextureCompat;
import net.minecraft.client.texture.NativeImage;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

public final class PanelBackground {

    private static final Identifier ID = Identifier.of("e33chat", "panel_bg_custom");

    private static final Object LOCK = new Object();
    private static String loadedKey = null;
    private static boolean registered;
    private static boolean failed;
    private static boolean warnedPath;
    private static int texW, texH;

    private PanelBackground() {}

    public static boolean available() {
        return registered && !failed && texW > 0 && texH > 0;
    }

    public static boolean loading() {
        synchronized (LOCK) {
            return loadedKey != null && !loadedKey.isEmpty() && !registered && !failed;
        }
    }

    public static boolean failed() {
        synchronized (LOCK) {
            return failed;
        }
    }

    public static void ensureLoaded() {
        String raw = ChatBubbleClientSetup.config().panelBgImage();
        String key = raw == null ? "" : raw.trim();
        synchronized (LOCK) {
            if (key.equals(loadedKey)) return;

            unloadLocked();
            loadedKey = key;
            failed = false;
            warnedPath = false;
            if (key.isEmpty()) return;
        }
        File file = resolve(key);
        if (file == null || !file.isFile()) {
            synchronized (LOCK) {
                failed = true;
                if (!warnedPath) {
                    warnedPath = true;
                    E33Log.info("[e33chat] panel background not found: {}", key);
                }
            }
            return;
        }
        final String forKey = key;
        com.niuqu.chatbubble.image.ImageLoader.executor().execute(() -> {
            NativeImage img = null;
            try (FileInputStream in = new FileInputStream(file)) {
                img = NativeImage.read(in);
                if (img.getWidth() <= 0 || img.getHeight() <= 0) throw new IllegalStateException("empty image");
                final NativeImage decoded = img;
                img = null;
                MinecraftClient.getInstance().execute(() -> apply(forKey, decoded));
            } catch (Throwable t) {
                if (img != null) img.close();
                synchronized (LOCK) {
                    failed = true;
                    if (!warnedPath) {
                        warnedPath = true;
                        E33Log.info("[e33chat] panel background load failed: {} -> {}", forKey, t.toString());
                    }
                }
            }
        });
    }

    private static void apply(String forKey, NativeImage decoded) {
        synchronized (LOCK) {
            if (!forKey.equals(loadedKey)) {
                decoded.close();
                return;
            }
            try {
                NativeImageBackedTexture tex = TextureCompat.create("e33chat_panel_bg", decoded);
                MinecraftClient.getInstance().getTextureManager().registerTexture(ID, tex);
                texW = decoded.getWidth();
                texH = decoded.getHeight();
                registered = true;
                failed = false;
            } catch (Throwable t) {
                decoded.close();
                failed = true;
                E33Log.info("[e33chat] panel background upload failed: {}", t.toString());
            }
        }
    }

    private static void unloadLocked() {
        if (registered) {
            try {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(ID);
            } catch (Throwable ignored) {}
        }
        registered = false;
        texW = 0;
        texH = 0;
    }

    private static File resolve(String path) {
        try {
            Path p = Path.of(path);
            if (!p.isAbsolute()) {
                p = FabricLoader.getInstance().getGameDir().resolve(path);
            }
            return p.toFile();
        } catch (Throwable t) {
            return null;
        }
    }

    public record Crop(float centerX, float centerY, float zoom) {

        public static final Crop DEFAULT = new Crop(0.5f, 0.5f, 1f);
    }

    public static Crop parseCrop(String raw) {
        if (raw == null || raw.isBlank()) return Crop.DEFAULT;
        String[] parts = raw.trim().split(",");
        if (parts.length != 3) return Crop.DEFAULT;
        try {
            float cx = Float.parseFloat(parts[0].trim());
            float cy = Float.parseFloat(parts[1].trim());
            float zoom = Float.parseFloat(parts[2].trim());
            if (!Float.isFinite(cx) || !Float.isFinite(cy) || !Float.isFinite(zoom)) return Crop.DEFAULT;
            return new Crop(clamp01(cx), clamp01(cy), Math.max(1f, zoom));
        } catch (NumberFormatException e) {
            return Crop.DEFAULT;
        }
    }

    public static String formatCrop(Crop crop) {
        return String.format(java.util.Locale.ROOT, "%.4f,%.4f,%.4f",
            crop.centerX(), crop.centerY(), crop.zoom());
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static int[] sourceRect(int texW, int texH, int targetW, int targetH, Crop crop) {
        if (texW <= 0 || texH <= 0 || targetW <= 0 || targetH <= 0) return new int[]{0, 0, 1, 1};
        float targetAspect = (float) targetW / targetH;
        float srcW = texW, srcH = texH;

        if ((float) texW / texH > targetAspect) {
            srcW = texH * targetAspect;
        } else {
            srcH = texW / targetAspect;
        }

        float zoom = Math.max(1f, crop.zoom());
        srcW /= zoom;
        srcH /= zoom;
        float minZoom = Math.max(srcW / texW, srcH / texH);
        if (minZoom > 1f) {
            srcW /= minZoom;
            srcH /= minZoom;
        }
        int w = Math.max(1, Math.round(srcW));
        int h = Math.max(1, Math.round(srcH));

        int u = Math.round(crop.centerX() * texW - w / 2f);
        int v = Math.round(crop.centerY() * texH - h / 2f);
        u = Math.max(0, Math.min(u, texW - w));
        v = Math.max(0, Math.min(v, texH - h));
        return new int[]{u, v, w, h};
    }

    public static int imageWidth() { return texW; }
    public static int imageHeight() { return texH; }

    public static Identifier textureId() { return available() ? ID : null; }

    private static volatile float lastAspect = 0.4f;
    public static float lastTargetAspect() { return lastAspect; }

    public static void draw(net.minecraft.client.gui.DrawContext g,
                            int x, int y, int w, int h, float alpha) {
        if (w > 0 && h > 0) lastAspect = (float) w / h;
        if (!available() || w <= 0 || h <= 0 || alpha <= 0.003f) return;
        int[] src = sourceRect(texW, texH, w, h, parseCrop(ChatBubbleClientSetup.config().panelBgCrop()));
        com.niuqu.chatbubble.texture.ColoredTextureRenderer.drawWithAlpha(
            g, ID, x, y, w, h, src[0], src[1], src[2], src[3], texW, texH, alpha);
    }
}
