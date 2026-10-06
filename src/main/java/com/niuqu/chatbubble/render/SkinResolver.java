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

/**
 * 玩家头像皮肤解析 + 离线判定。
 *
 * <p>两张 LRU 缓存（UUID / 去 § 小写名字）只保存玩家的<b>真实</b>皮肤，
 * Steve/Alex 占位皮一律不入缓存。这是「玩家下线后头像变成默认皮肤」的根因：
 * 皮肤还在异步下载（或玩家已退服）时 {@code PlayerListEntry} 会给出占位皮，
 * 一旦把占位皮写进缓存就再也换不回真实皮肤。</p>
 *
 * <p>离线玩家（不在 Tab 列表里，或只在聊天记录里出现过）由
 * {@link #isOffline(UUID, String)} 判定，渲染时按灰显样式绘制。</p>
 */
public final class SkinResolver {
    private SkinResolver() {}

    private static final int CACHE_CAP = 256;
    private static final UUID NIL_UUID = new UUID(0, 0);

    /** 离线头像压暗系数与不透明度系数（全工程唯一的灰显参数）。 */
    private static final float OFFLINE_LEVEL = 0.45f;
    private static final float OFFLINE_ALPHA = 0.8f;

    /** 皮肤贴图里 face 层与 hat 层的 8×8 UV 原点（64×64 皮肤布局）。 */
    private static final float FACE_U = 8f;
    private static final float FACE_V = 8f;
    private static final float HAT_U = 40f;
    private static final float HAT_V = 8f;
    private static final int HEAD_PX = 8;
    private static final int SKIN_PX = 64;

    private static final Map<UUID, Identifier> byUuid = newLru();
    private static final Map<String, Identifier> byName = newLru();

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

    /**
     * 是否为原版 Steve/Alex 占位皮（{@code null} 也算：1.16.5 的
     * {@code getSkinTexture()} 对默认皮肤返回 null）。
     *
     * <p>真实皮肤（含皮肤站 / CustomSkinLoader 注册的）永远是
     * {@code <命名空间>:skins/<hash>}，而占位皮是随版本打包的资源
     * {@code minecraft:textures/entity/steve.png}（1.16.5）或
     * {@code minecraft:textures/entity/player/{wide,slim}/…}（1.17+）。
     * 按 Identifier 前缀判定即可，既覆盖 Steve / Alex 两种模型，也避开各版本
     * {@code DefaultSkinHelper} 签名差异，还不会把名字里带 steve/alex 的玩家误判。</p>
     */
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

    /** 缓存里该玩家的真实皮肤（UUID 优先，其次按名字兜底）。 */
    private static Identifier cached(UUID uuid, String name) {
        if (uuid != null && !uuid.equals(NIL_UUID)) {
            Identifier hit = byUuid.get(uuid);
            if (hit != null) return hit;
        }
        String key = nameKey(name);
        return key != null ? byName.get(key) : null;
    }

    /**
     * 玩家是否已离线（不在 Tab 列表里）。离线玩家的头像灰显，而不是退回默认皮肤。
     *
     * <p>断线过程中 / 不在世界里时返回 false，避免加载界面或退服瞬间头像集体变灰。</p>
     */
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

        // 在线：每帧重新读 PlayerListEntry（缓存首次结果会把下载中的占位皮冻住）。
        // 读到占位皮时回落到缓存里的真实皮肤，并且绝不写缓存。
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

        // 离线：直接用缓存里的真实皮肤——这就是「下线不退回默认皮肤」。
        Identifier hit = cached(uuid, name);
        if (hit != null) return hit;

        // 一次都没见过：交给皮肤站解析（CustomSkinLoader 能按名字解析离线玩家），
        // 同样只有真实皮肤才写缓存。
        Identifier resolved = resolveSkin(uuid, name);
        remember(uuid, name, resolved);
        return resolved;
    }

    /**
     * 画一个玩家头像（face 层 + hat 层）。这是全工程唯一的头像绘制入口：
     * UV 常量、离线灰显参数都只在这里出现一次。
     *
     * @param offline 已离线（{@link #isOffline}）——头像压暗显示，而不是换成默认皮肤
     */
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

    // ==== 解析防抖 ====
    // 缓存未命中（离线玩家/聊天记录里首次出现的名字）时每帧都会走到 resolveSkin。
    // 旧实现在 1.21.9+ 用阻塞 .get() 卡渲染线程等皮肤下载；这里同一玩家一次只发起
    // 一次解析，等待期直接画占位皮，结果异步回填缓存。
    private static final java.util.Map<String, Long> RESOLVE_INFLIGHT =
        new java.util.concurrent.ConcurrentHashMap<>();
    //#if MC < 12002
    //$$ // 1.16.5 的 getTextures 可能同步查 session 服务器（阻塞调用线程）：20s 限频
    //$$ private static final long RESOLVE_RETRY_MS = 20_000L;
    //#else
    // 1.20.2+ 的 getSkinTextures / supplySkinTextures 内部自行异步下载、查表即回：
    // 1s 重试可在下载完成后尽快拿到真皮肤，同时把每帧调用限成每秒一次
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
                // 带名字的 GameProfile 走 PlayerSkinProvider，CustomSkinLoader 才能
                // 按名字匹配离线玩家的本地皮肤（等价于 Mojang 映射的 getInsecureSkin）。
                GameProfile profile = new GameProfile(id, name);
                //#if MC >= 12109
                // 1.21.9 的 supplySkinTextures 返回 Supplier<SkinTextures>（同步取当前值）
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

    /** 该玩家的原版占位皮（真实皮肤一次都没见过时的兜底）。 */
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
