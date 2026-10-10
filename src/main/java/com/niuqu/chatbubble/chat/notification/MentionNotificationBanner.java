package com.niuqu.chatbubble.chat.notification;

import com.niuqu.chatbubble.render.Animation;
import com.niuqu.chatbubble.render.AnimationStyle;
import com.niuqu.chatbubble.render.Appearance;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.render.RoundRectRenderer;
import com.niuqu.chatbubble.render.SkinResolver;
import com.niuqu.chatbubble.render.UiTokens;
import net.minecraft.client.MinecraftClient;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.*;

public class MentionNotificationBanner {
    public static final MentionNotificationBanner INSTANCE = new MentionNotificationBanner();

    public enum NotificationType { MENTION, QUOTE, WHISPER, SYSTEM }

    private static final long VISIBLE_MS_PERIOD = 1000;
    private static final long SLIDE_IN_MS = 250;
    private static final long PUSH_MS = 200;
    private static final long EXIT_MS = 150;
    private static final int AVATAR = 24;
    private static final int AVATAR_HAT = 26;
    private static final int AVATAR_X = 8;
    private static final int TEXT_X = AVATAR_X + AVATAR + 6;
    private static final int TEXT_X_PLAIN = 8;
    private static final int MAX_TEXT_W = 170;
    private static final int BANNER_H = 36;
    private static final int MAX_MSG_LINES = 2;
    private static final int SHADOW_OFF = UiTokens.SHADOW_OFFSET_PANEL;
    private static final float COMPACT_SCALE = 0.75f;

    private static final float STACK_OVERLAP = 0.5f;

    private final List<ActiveBanner> banners = new ArrayList<>();
    private final List<ExitingBanner> exiting = new ArrayList<>();

    private MentionNotificationBanner() {}

    public void enqueue(UUID senderUUID, Text senderName, Text content,
                        int messageIndex, NotificationType type) {
        MinecraftClient mc = MinecraftClient.getInstance();

        String prefix = switch (type) {
            case MENTION -> Text.translatable("e33chat.banner.mention").getString();
            case QUOTE -> Text.translatable("e33chat.banner.quote").getString();
            case WHISPER -> Text.translatable("e33chat.banner.whisper").getString();
            case SYSTEM -> Text.translatable("e33chat.banner.system").getString();
        };
        Text labeledName = Text.literal(prefix).append(senderName);

        boolean hasAvatar = type != NotificationType.SYSTEM;
        int textOriginX = hasAvatar ? TEXT_X : TEXT_X_PLAIN;
        int maxTextW = Math.min(MAX_TEXT_W, mc.getWindow().getScaledWidth() - textOriginX - 12);
        int dotsW = mc.textRenderer.getWidth("...");
        int contentMaxW = hasAvatar ? maxTextW : Math.max(40, maxTextW - mc.textRenderer.getWidth(prefix));

        List<OrderedText> nameLines = mc.textRenderer.wrapLines(labeledName, maxTextW);
        OrderedText nameSeq;
        if (nameLines.isEmpty()) {
            nameSeq = OrderedText.EMPTY;
        } else if (nameLines.size() > 1) {
            String plainName = mc.textRenderer.trimToWidth(
                labeledName.getString(), maxTextW - dotsW) + "...";
            nameSeq = mc.textRenderer.wrapLines(Text.literal(plainName), maxTextW).get(0);
        } else {
            nameSeq = nameLines.get(0);
        }

        List<OrderedText> msgLines = mc.textRenderer.wrapLines(content, contentMaxW);
        if (msgLines.size() > MAX_MSG_LINES) {
            msgLines = mc.textRenderer.wrapLines(truncateStyled(content, contentMaxW * 2 - dotsW, mc.textRenderer, "..."), contentMaxW);
            if (msgLines.size() > MAX_MSG_LINES)
                msgLines = msgLines.subList(0, MAX_MSG_LINES);
        }

        int textW = mc.textRenderer.getWidth(nameSeq);
        for (var line : msgLines) textW = Math.max(textW, mc.textRenderer.getWidth(line));
        if (!hasAvatar && !msgLines.isEmpty()) {
            textW = Math.max(textW, mc.textRenderer.getWidth(nameSeq) + mc.textRenderer.getWidth(msgLines.get(0)));
        }
        int bannerW = textOriginX + textW + 12;

        int bannerH = hasAvatar ? BANNER_H
            : mc.textRenderer.fontHeight * msgLines.size() + 10;

        PendingBanner pb = new PendingBanner(senderUUID, senderName, content, messageIndex,
            type, hasAvatar, nameSeq, msgLines, textW, bannerW, bannerH);
        addBanner(pb);
    }

    public int pendingCount() { return banners.size() + exiting.size(); }

    public void tick() {
        long now = System.currentTimeMillis();

        List<ActiveBanner> expired = new ArrayList<>();
        for (ActiveBanner b : banners) {
            if (now >= b.totalVisibleMs) expired.add(b);
        }
        if (!expired.isEmpty()) {
            for (ActiveBanner b : banners) {
                b.fromY = currentY(b, now);
                b.fromScale = currentScale(b, now);
            }
            for (ActiveBanner b : expired) {
                float y = b.fromY;
                float scale = b.fromScale;
                banners.remove(b);
                exiting.add(new ExitingBanner(b.data, now, y, scale));
            }
            for (ActiveBanner b : banners) {
                b.enterStartMs = -1;
                b.pushStartMs = now;
            }
        }

        exiting.removeIf(e -> now - e.startMs >= EXIT_MS);

        if (!banners.isEmpty() || !exiting.isEmpty()) {
            ChatMessageStore.debugLog(() -> "[e33chat] Banner stack | visible=" + banners.size()
                + " | exiting=" + exiting.size());
        }
    }

    public void render(DrawContext g, int screenW, int screenH) {
        if (!ChatBubbleClientSetup.config().mentionBannerEnabled()) return;
        if (banners.isEmpty() && exiting.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();

        AnimationStyle exitStyle = AnimationStyle.parse(ChatBubbleClientSetup.config().bannerAnimStyle());
        for (ExitingBanner e : exiting) {
            float t = Math.min(1f, (float) (now - e.startMs) / EXIT_MS);
            float y = e.y;
            float scale = e.scale;
            float alpha;
            if (exitStyle == AnimationStyle.NONE) {
                alpha = 0f;
            } else if (exitStyle == AnimationStyle.SLIDE) {
                float ease = t * t;
                y = e.y - (e.data.bannerH * e.scale) * ease;
                alpha = exitFade(1f - t);
            } else if (exitStyle == AnimationStyle.FADE) {
                alpha = exitFade(1f - t);
            } else {
                alpha = exitFade(1f - t);
                scale = e.scale * (1f - 0.5f * t);
            }
            renderBanner(g, e.data, screenW, y, scale, alpha);
        }

        for (int i = banners.size() - 1; i >= 0; i--) {
            ActiveBanner b = banners.get(i);
            float y = currentY(b, now);
            float scale = currentScale(b, now);
            float alpha = currentAlpha(b, now);
            renderBanner(g, b.data, screenW, y, scale, alpha);
        }
    }

    public int currentMessageIndex() {
        return banners.isEmpty() ? -1 : banners.get(0).data.messageIndex;
    }

    private void addBanner(PendingBanner pb) {
        long now = System.currentTimeMillis();
        int maxStack = maxStack();

        exiting.clear();

        for (ActiveBanner b : banners) {
            b.fromY = currentY(b, now);
            b.fromScale = currentScale(b, now);
        }

        if (banners.size() >= maxStack && !banners.isEmpty()) {
            ActiveBanner oldest = banners.get(banners.size() - 1);
            exiting.add(new ExitingBanner(oldest.data, now, oldest.fromY, oldest.fromScale));
            banners.remove(banners.size() - 1);
        }

        ActiveBanner nb = new ActiveBanner(pb, now, now + visibleDurationMs(), now);
        banners.add(0, nb);

        for (ActiveBanner b : banners) {
            if (b != nb) {
                b.enterStartMs = -1;
                b.pushStartMs = now;
            }
        }
    }

    private long visibleDurationMs() {
        return (long) ChatBubbleClientSetup.config().mentionBannerDuration() * VISIBLE_MS_PERIOD;
    }

    private int maxStack() {
        Integer v = ChatBubbleClientSetup.config().bannerMaxStack();
        int value = v != null ? v : 3;
        return Math.max(1, Math.min(5, value));
    }

    private float targetY(int index, ActiveBanner b) {
        float y = ChatBubbleClientSetup.config().bannerOffsetY();
        for (int i = 0; i < index; i++) {
            ActiveBanner prev = banners.get(i);
            float prevScale = i == 0 ? 1f : COMPACT_SCALE;
            y += prev.data.bannerH * prevScale * STACK_OVERLAP;
        }
        return y;
    }

    private float targetScale(int index) {
        return index == 0 ? 1f : COMPACT_SCALE;
    }

    private float currentY(ActiveBanner b, long now) {
        int i = banners.indexOf(b);
        if (i < 0) return ChatBubbleClientSetup.config().bannerOffsetY();
        float target = targetY(i, b);
        if (b.pushStartMs >= 0) {
            float t = Math.min(1f, (float) (now - b.pushStartMs) / PUSH_MS);
            float e = Animation.easeOutCubic(t);
            return b.fromY + (target - b.fromY) * e;
        }
        if (i == 0 && b.enterStartMs >= 0) {
            long elapsed = now - b.enterStartMs;
            if (elapsed < SLIDE_IN_MS) {
                float raw = Math.min(1f, (float) elapsed / SLIDE_IN_MS);
                AnimationStyle bstyle = AnimationStyle.parse(ChatBubbleClientSetup.config().bannerAnimStyle());
                if (bstyle == AnimationStyle.SLIDE) {
                    float c = 1.70158f;
                    float slide = 1f + c * (float) Math.pow(raw - 1, 3) + c * (float) Math.pow(raw - 1, 2);
                    return (-b.data.bannerH) + slide * b.data.bannerH + ChatBubbleClientSetup.config().bannerOffsetY();
                }
            }
        }
        return target;
    }

    private float currentScale(ActiveBanner b, long now) {
        int i = banners.indexOf(b);
        if (i < 0) return 1f;
        float target = targetScale(i);
        if (b.pushStartMs >= 0) {
            float t = Math.min(1f, (float) (now - b.pushStartMs) / PUSH_MS);
            float e = Animation.easeOutCubic(t);
            return b.fromScale + (target - b.fromScale) * e;
        }
        if (i == 0 && b.enterStartMs >= 0) {
            long elapsed = now - b.enterStartMs;
            if (elapsed < SLIDE_IN_MS) {
                float raw = Math.min(1f, (float) elapsed / SLIDE_IN_MS);
                AnimationStyle bstyle = AnimationStyle.parse(ChatBubbleClientSetup.config().bannerAnimStyle());
                if (bstyle == AnimationStyle.ZOOM) {
                    return 0.8f + 0.2f * Animation.easeOutBack(raw);
                }
            }
        }
        return target;
    }

    private float currentAlpha(ActiveBanner b, long now) {
        if (b.pushStartMs >= 0) return 1f;
        int i = banners.indexOf(b);
        if (i == 0 && b.enterStartMs >= 0) {
            long elapsed = now - b.enterStartMs;
            if (elapsed < SLIDE_IN_MS) {
                float raw = Math.min(1f, (float) elapsed / SLIDE_IN_MS);
                AnimationStyle bstyle = AnimationStyle.parse(ChatBubbleClientSetup.config().bannerAnimStyle());
                if (bstyle == AnimationStyle.NONE) return 1f;
                if (bstyle == AnimationStyle.SLIDE) return Math.min(1f, raw / 0.6f);
                return Animation.easeOutQuad(raw);
            }
        }
        return 1f;
    }

    private void renderBanner(DrawContext g, PendingBanner b, int screenW,
                              float y, float scale, float alpha) {
        if (alpha <= 0.003f) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        float bgAlphaMul = alpha * (ChatBubbleClientSetup.config().bannerOpacity() / 100f);

        var theme = Appearance.snapshot();
        int bg = theme.bannerBg();
        int cornerRadius = ChatBubbleClientSetup.config().bannerCornerRadius();

        int bannerW = b.bannerW;
        int bannerH = b.bannerH;
        int x = (screenW - bannerW) / 2 + ChatBubbleClientSetup.config().bannerOffsetX();
        int iy = (int) y;

        if (scale != 1f) {
            g.getMatrices().push();
            g.getMatrices().translate(x + bannerW / 2f, y + bannerH / 2f, 0);
            g.getMatrices().scale(scale, scale, 1f);
            g.getMatrices().translate(-(x + bannerW / 2f), -(y + bannerH / 2f), 0);
        }

        int shadowAlpha = (int) (UiTokens.SHADOW_ALPHA_PANEL * bgAlphaMul);
        int shadowColor = (shadowAlpha << 24);
        RoundRectRenderer.fill(g, x + SHADOW_OFF, iy + SHADOW_OFF,
            x + bannerW + SHADOW_OFF, iy + bannerH + SHADOW_OFF, cornerRadius, shadowColor);

        int bgAlpha = (int) ((bg >>> 24) * bgAlphaMul);
        RoundRectRenderer.fill(g, x, iy, x + bannerW, iy + bannerH, cornerRadius,
            (bgAlpha << 24) | (bg & 0x00FFFFFF));

        int textX = x + (b.hasAvatar ? TEXT_X : TEXT_X_PLAIN);

        List<OrderedText> drawLines = scale < 1f && b.msgLines.size() > 1
            ? b.msgLines.subList(0, 1) : b.msgLines;
        int nameColor, msgColor;
        if (b.hasAvatar) {
            int avatarY = iy + (bannerH - AVATAR_HAT) / 2;
            String senderName = b.senderName.getString();
            SkinResolver.drawAvatar(g, b.senderUUID, senderName, x + AVATAR_X, avatarY,
                AVATAR, AVATAR_HAT, alpha, SkinResolver.isOffline(b.senderUUID, senderName));

            int nameY = iy + 6;
            int nameAlpha = (int) ((theme.textPrimary() >>> 24) * alpha);
            nameColor = (nameAlpha << 24) | (theme.textPrimary() & 0x00FFFFFF);
            g.drawText(mc.textRenderer, b.nameSeq, textX, nameY, nameColor, false);

            int msgAlpha = (int) ((theme.textSecondary() >>> 24) * alpha);
            msgColor = (msgAlpha << 24) | (theme.textSecondary() & 0x00FFFFFF);
            int msgY = nameY + mc.textRenderer.fontHeight + 2;
            for (int i = 0; i < drawLines.size(); i++)
                g.drawText(mc.textRenderer, drawLines.get(i), textX,
                    msgY + i * mc.textRenderer.fontHeight, msgColor, false);
        } else {
            int nameAlpha = (int) ((theme.textPrimary() >>> 24) * alpha);
            nameColor = (nameAlpha << 24) | (theme.textPrimary() & 0x00FFFFFF);
            int msgAlpha = (int) ((theme.textSecondary() >>> 24) * alpha);
            msgColor = (msgAlpha << 24) | (theme.textSecondary() & 0x00FFFFFF);
            int lineH = mc.textRenderer.fontHeight;
            int totalH = lineH * drawLines.size();
            int textY = iy + (bannerH - totalH) / 2;
            g.drawText(mc.textRenderer, b.nameSeq, textX, textY, nameColor, false);
            int contentX = textX + mc.textRenderer.getWidth(b.nameSeq);
            g.drawText(mc.textRenderer, drawLines.get(0), contentX, textY, msgColor, false);
            for (int i = 1; i < drawLines.size(); i++)
                g.drawText(mc.textRenderer, drawLines.get(i), textX,
                    textY + i * lineH, msgColor, false);
        }

        if (scale != 1f) g.getMatrices().pop();
    }

    private static float exitFade(float raw) {
        return raw * (2f - raw);
    }

    private static Text truncateStyled(Text src, int maxWidth,
                                       net.minecraft.client.font.TextRenderer font, String suffix) {
        int budget = maxWidth - font.getWidth(suffix);
        MutableText out = Text.empty();
        int[] used = {0};
        src.visit((style, text) -> {
            if (used[0] >= budget) return java.util.Optional.<Object>empty();
            int w = font.getWidth(text);
            if (used[0] + w <= budget) {
                out.append(Text.literal(text).fillStyle(style));
                used[0] += w;
            } else {
                String sub = font.trimToWidth(text, budget - used[0]);
                out.append(Text.literal(sub).fillStyle(style));
                used[0] = budget;
            }
            return java.util.Optional.<Object>empty();
        }, net.minecraft.text.Style.EMPTY);
        return out.append(Text.literal(suffix));
    }

    private static final class ActiveBanner {
        final PendingBanner data;
        final long bornMs;
        final long totalVisibleMs;
        long enterStartMs;
        long pushStartMs = -1;
        float fromY;
        float fromScale;

        ActiveBanner(PendingBanner data, long bornMs, long totalVisibleMs, long enterStartMs) {
            this.data = data;
            this.bornMs = bornMs;
            this.totalVisibleMs = totalVisibleMs;
            this.enterStartMs = enterStartMs;
        }
    }

    private static final class ExitingBanner {
        final PendingBanner data;
        final long startMs;
        final float y;
        final float scale;

        ExitingBanner(PendingBanner data, long startMs, float y, float scale) {
            this.data = data;
            this.startMs = startMs;
            this.y = y;
            this.scale = scale;
        }
    }

    private record PendingBanner(UUID senderUUID, Text senderName, Text content,
                                  int messageIndex, NotificationType type, boolean hasAvatar,
                                  OrderedText nameSeq, List<OrderedText> msgLines,
                                  int textW, int bannerW, int bannerH) {}
}
