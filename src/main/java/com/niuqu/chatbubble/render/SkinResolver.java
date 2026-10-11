package com.niuqu.chatbubble.render;

import com.mojang.authlib.GameProfile;
import com.niuqu.chatbubble.texture.ColoredTextureRenderer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif

public final class SkinResolver {
    private SkinResolver() {}

    private static final int CACHE_CAP = 1024;
    private static final UUID NIL_UUID = new UUID(0, 0);

    private static final float OFFLINE_LEVEL = 0.45f;
    private static final float OFFLINE_ALPHA = 0.8f;

    private static final float FACE_U = 8f;
    private static final float FACE_V = 8f;
    private static final float HAT_U = 40f;
    private static final float HAT_V = 8f;
    private static final int HEAD_PX = 8;
    private static final int SKIN_PX = 64;

    private static final Map<UUID, Identifier> byUuid = newLru();
    private static final Map<String, Identifier> byName = newLru();

    // ==== 下线头像保护：消息到达即预热皮肤缓存 ====
    // 皮肤缓存只在聊天面板渲染消息时（getSkin）填充。面板关闭期间某玩家聊天、
    // 随后下线：皮肤从未进缓存，再打开面板时人已离线拿不到皮肤，头像退化为
    // 默认皮肤。修复：addMessage 把 (uuid, name) 投入并发队列，客户端 tick 在
    // 主线程消费——玩家在线且皮肤已下载完时入缓存；下载未完则留在队列下帧重试，
    // 玩家离线（列表条目消失）则放弃。LRU 上限同时 256 → 1024（大服淘汰过快）。
    private record Prewarm(UUID uuid, String name) {}
    private static final java.util.Queue<Prewarm> PREWARM_QUEUE =
        new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** 任意线程可调用（旧版本路径的消息在网络线程到达）。 */
    public static void enqueuePrewarm(UUID uuid, String name) {
        if (uuid == null || uuid.equals(NIL_UUID) || name == null || name.isEmpty()) return;
        PREWARM_QUEUE.offer(new Prewarm(uuid, name));
    }

    /** 主线程（client tick）：把队列里的在线玩家皮肤刷入缓存。 */
    public static void tickPrewarm() {
        if (PREWARM_QUEUE.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int budget = 200;
        Prewarm p;
        while ((p = PREWARM_QUEUE.poll()) != null) {
            if (--budget < 0) { PREWARM_QUEUE.offer(p); return; }
            if (cached(p.uuid(), p.name()) != null) continue;   // 已缓存
            if (client.getNetworkHandler() == null) continue;   // 已断线：放弃
            PlayerListEntry info = client.getNetworkHandler().getPlayerListEntry(p.uuid());
            if (info == null) continue;                         // 玩家已离线：放弃
            Identifier tex = entrySkin(info);
            remember(p.uuid(), p.name(), tex);                  // 占位皮会被 remember 丢弃
            if (isPlaceholder(tex)) PREWARM_QUEUE.offer(p);     // 皮肤仍在下载：下帧重试
        }
    }


    private static <K> Map<K, Identifier> newLru() {
        return new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Identifier> eldest) {
                return size() > CACHE_CAP;
            }
        };
    }

    private static final java.util.regex.Pattern SECTION_CODE =
        java.util.regex.Pattern.compile("§.");

    private static String nameKey(String name) {
        if (name == null) return null;
        String key = SECTION_CODE.matcher(name).replaceAll("").trim().toLowerCase(Locale.ROOT);
        return key.isEmpty() ? null : key;
    }

    private static boolean isPlaceholder(Identifier tex) {
        if (tex == null) return true;
        return "minecraft".equals(tex.getNamespace()) && tex.getPath().startsWith("textures/entity/");
    }

    private static void remember(UUID uuid, String name, Identifier tex) {
        if (isPlaceholder(tex)) return;
        if (uuid != null && !uuid.equals(NIL_UUID)) byUuid.put(uuid, tex);
        String key = nameKey(name);
        if (key != null) byName.put(key, tex);
    }

    private static Identifier cached(UUID uuid, String name) {
        if (uuid != null && !uuid.equals(NIL_UUID)) {
            Identifier hit = byUuid.get(uuid);
            if (hit != null) return hit;
        }
        String key = nameKey(name);
        return key != null ? byName.get(key) : null;
    }

    public static boolean isOffline(UUID uuid, String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (BlurRenderer.isDisconnecting() || client.world == null || client.getNetworkHandler() == null) {
            return false;
        }
        if (uuid != null && !uuid.equals(NIL_UUID)) {
            return client.getNetworkHandler().getPlayerListEntry(uuid) == null;
        }
        if (name != null && !name.isEmpty()) {
            //#if MC >= 12109
            return client.getNetworkHandler().getPlayerListEntry(name) == null;
            //#else
            //$$ for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            //$$     GameProfile profile = entry.getProfile();
            //$$     if (profile != null && name.equals(profile.getName())) return false;
            //$$ }
            //$$ return true;
            //#endif
        }
        return false;
    }

    public static Identifier getSkin(UUID uuid, String name) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() != null && uuid != null && !uuid.equals(NIL_UUID)) {
            PlayerListEntry info = client.getNetworkHandler().getPlayerListEntry(uuid);
            if (info != null) {
                Identifier tex = entrySkin(info);
                if (!isPlaceholder(tex)) {
                    remember(uuid, name, tex);
                    return tex;
                }
                Identifier hit = cached(uuid, name);
                return hit != null ? hit : tex;
            }
        }

        Identifier hit = cached(uuid, name);
        if (hit != null) return hit;

        Identifier resolved = resolveSkin(uuid, name);
        remember(uuid, name, resolved);
        return resolved;
    }

    public static void drawAvatar(DrawContext g, UUID uuid, String name,
                                  int x, int y, int baseSize, int hatSize,
                                  float alpha, boolean offline) {
        if (g == null || alpha <= 0.003f) return;
        Identifier skin = getSkin(uuid, name);
        if (skin == null) return;
        float level = offline ? OFFLINE_LEVEL : 1f;
        float a = offline ? alpha * OFFLINE_ALPHA : alpha;
        ColoredTextureRenderer.drawWithAlphaGrayscale(g, skin, x, y, baseSize, baseSize,
            FACE_U, FACE_V, HEAD_PX, HEAD_PX, SKIN_PX, SKIN_PX, a, level);
        int hatOff = (hatSize - baseSize) / 2;
        ColoredTextureRenderer.drawWithAlphaGrayscale(g, skin, x - hatOff, y - hatOff, hatSize, hatSize,
            HAT_U, HAT_V, HEAD_PX, HEAD_PX, SKIN_PX, SKIN_PX, a, level);
    }

    private static Identifier entrySkin(PlayerListEntry info) {
        //#if MC >= 12109
        return info.getSkinTextures().body().texturePath();
        //#else
        //#if MC >= 12002
        //$$ return info.getSkinTextures().texture();
        //#else
        //$$ return info.getSkinTexture();
        //#endif
        //#endif
    }

    private static final java.util.Map<String, Long> RESOLVE_INFLIGHT =
        new java.util.concurrent.ConcurrentHashMap<>();
    //#if MC < 12002
    //$$ // 1.16.5 的 getTextures 可能同步查 session 服务器（阻塞调用线程）：20s 限频
    //$$ private static final long RESOLVE_RETRY_MS = 20_000L;
    //#else

    private static final long RESOLVE_RETRY_MS = 1_000L;
    //#endif

    private static Identifier resolveSkin(UUID uuid, String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        UUID id = uuid != null && !uuid.equals(NIL_UUID) ? uuid : NIL_UUID;
        if (name != null && !name.isEmpty()) {
            String key = id + "|" + name;
            Long startedAt = RESOLVE_INFLIGHT.get(key);
            long now = System.currentTimeMillis();
            if (startedAt != null && now - startedAt < RESOLVE_RETRY_MS) {
                return defaultSkin(id, name);
            }
            RESOLVE_INFLIGHT.put(key, now);
            try {
                GameProfile profile = new GameProfile(id, name);
                //#if MC >= 12109

                Identifier tex = client.getSkinProvider().supplySkinTextures(profile, false).get().body().texturePath();
                RESOLVE_INFLIGHT.remove(key);
                return tex;
                //#else
                //#if MC >= 12002
                //$$ Identifier tex = client.getSkinProvider().getSkinTextures(profile).texture();
                //$$ RESOLVE_INFLIGHT.remove(key);
                //$$ return tex;
                //#else
                //$$ java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type,
                //$$         com.mojang.authlib.minecraft.MinecraftProfileTexture> textures =
                //$$     client.getSkinProvider().getTextures(profile);
                //$$ com.mojang.authlib.minecraft.MinecraftProfileTexture raw = textures == null
                //$$     ? null
                //$$     : textures.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
                //$$ Identifier tex = null;
                //$$ if (raw != null) {
                //$$     tex = client.getSkinProvider().loadSkin(
                //$$         raw, com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
                //$$ }
                //$$ RESOLVE_INFLIGHT.remove(key);
                //$$ if (tex != null) return tex;
                //#endif
                //#endif
            } catch (Throwable ignored) {
                RESOLVE_INFLIGHT.remove(key);
            }
        }
        return defaultSkin(id, name);
    }

    private static Identifier defaultSkin(UUID uuid, String name) {
        //#if MC >= 12109
        return net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(
            new GameProfile(uuid, name != null ? name : "")).body().texturePath();
        //#else
        //#if MC >= 12002
        //$$ return net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(uuid).texture();
        //#else
        //$$ return net.minecraft.client.util.DefaultSkinHelper.getTexture(uuid);
        //#endif
        //#endif
    }
}
