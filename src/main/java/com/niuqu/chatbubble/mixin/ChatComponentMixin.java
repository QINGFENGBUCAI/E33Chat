package com.niuqu.chatbubble.mixin;
import com.niuqu.chatbubble.store.EchoTracker;
import com.niuqu.chatbubble.store.BlockList;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.ChatBubbleScreen;
import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.store.ChatMessageStore.SenderMeta;
import com.niuqu.chatbubble.image.BracketCodec;
import net.minecraft.client.MinecraftClient;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif
//#if MC >= 12111
import net.minecraft.client.font.TextRenderer;
//#endif
import net.minecraft.client.gui.hud.ChatHud;
//#if MC >= 11900
//#if MC < 26000
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
//#endif
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

// 接管聊天渲染与捕获
@Mixin(value = ChatHud.class, priority = 500)
public class ChatComponentMixin {
    private Text lastComponent;
    private boolean e33chat$shifted;
    private boolean e33chat$reposting;
    private String lastRepostText;
    private long lastRepostTime;

    //#if MC >= 26000
    // 气泡界面拦截渲染
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        if (ChatBubbleClientSetup.config().enabled()
                && MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
            ci.cancel();
        }
    }
    //#else
    //#if MC >= 12111
    // 气泡界面拦截渲染
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, TextRenderer textRenderer, int tickDelta, int mouseX, int mouseY,
                          boolean focused, boolean unread, CallbackInfo ci) {
        e33chat$shifted = false;
        if (ChatBubbleClientSetup.config().enabled()) {
            if (MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
                ci.cancel();
                return;
            }
            context.getMatrices().push();
            context.getMatrices().translate(0, -8, 0);
            e33chat$shifted = true;
        }
    }

    // 恢复渲染位移
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, TextRenderer textRenderer, int tickDelta, int mouseX, int mouseY,
                                boolean focused, boolean unread, CallbackInfo ci) {
        if (e33chat$shifted) {
            context.getMatrices().pop();
        }
    }
    //#else
    //#if MC >= 12100
    // 气泡界面拦截渲染
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int tickDelta, int mouseX, int mouseY,
                          boolean focused, CallbackInfo ci) {
        e33chat$shifted = false;
        if (ChatBubbleClientSetup.config().enabled()) {
            if (MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
                ci.cancel();
                return;
            }
            context.getMatrices().push();
            context.getMatrices().translate(0, -8, 0);
            e33chat$shifted = true;
        }
    }

    // 恢复渲染位移
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, int tickDelta, int mouseX, int mouseY,
                                boolean focused, CallbackInfo ci) {
        if (e33chat$shifted) {
            context.getMatrices().pop();
        }
    }
    //#else
    //#if MC >= 12000
    // 气泡界面拦截渲染
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int tickDelta, int mouseX, int mouseY, CallbackInfo ci) {
        e33chat$shifted = false;
        if (ChatBubbleClientSetup.config().enabled()) {
            if (MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
                ci.cancel();
                return;
            }
            context.getMatrices().push();
            context.getMatrices().translate(0, -8, 0);
            e33chat$shifted = true;
        }
    }

    // 恢复渲染位移
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, int tickDelta, int mouseX, int mouseY, CallbackInfo ci) {
        if (e33chat$shifted) {
            context.getMatrices().pop();
        }
    }
    //#else
    //#if MC >= 11903
    // 气泡界面拦截渲染
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(net.minecraft.client.util.math.MatrixStack matrices, int tickDelta, int mouseX, int mouseY, CallbackInfo ci) {
        e33chat$shifted = false;
        if (ChatBubbleClientSetup.config().enabled()) {
            if (MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
                ci.cancel();
                return;
            }
            matrices.push();
            matrices.translate(0, -8, 0);
            e33chat$shifted = true;
        }
    }

    // 恢复渲染位移
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(net.minecraft.client.util.math.MatrixStack matrices, int tickDelta, int mouseX, int mouseY, CallbackInfo ci) {
        if (e33chat$shifted) {
            matrices.pop();
        }
    }
    //#else
    // 气泡界面拦截渲染
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(net.minecraft.client.util.math.MatrixStack matrices, int tickDelta, CallbackInfo ci) {
        e33chat$shifted = false;
        if (ChatBubbleClientSetup.config().enabled()) {
            if (MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
                ci.cancel();
                return;
            }
            matrices.push();
            matrices.translate(0, -8, 0);
            e33chat$shifted = true;
        }
    }

    // 恢复渲染位移
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(net.minecraft.client.util.math.MatrixStack matrices, int tickDelta, CallbackInfo ci) {
        if (e33chat$shifted) {
            matrices.pop();
        }
    }
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif
    //#if MC >= 26000
    // 捕获新增消息
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, net.minecraft.network.chat.MessageSignature signature,
                              net.minecraft.client.multiplayer.chat.GuiMessageSource source,
                              net.minecraft.client.multiplayer.chat.GuiMessageTag tag, CallbackInfo ci) {
        captureMessage(message, ci);
    }
    //#else
    // 捕获新增消息
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        captureMessage(message, ci);
    }
    //#endif
    //#if MC >= 11900
    //#if MC < 26000
    // 捕获新增消息
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddMessageFull(Text message, MessageSignatureData signature,
                                  MessageIndicator indicator, CallbackInfo ci) {
        captureMessage(message, ci);
    }
    //#endif
    //#endif

    // 重投到原版聊天
    private void repostToVanilla(Text name, String content, boolean quoting) {
        Text tag = (quoting
            ? Text.translatable("e33chat.banner.quote").formatted(Formatting.YELLOW)
            : Text.translatable("e33chat.banner.whisper").formatted(Formatting.LIGHT_PURPLE));
        Text reformatted = Text.empty()
            .append(Text.literal("<")).append(name).append(Text.literal(">")).append(tag)
            .append(Text.literal(content));
        String repostStr = reformatted.getString();
        long nowMs = System.currentTimeMillis();
        if (ChatMessageStore.isRepostDuplicate(lastRepostText, lastRepostTime, repostStr, nowMs)) {
            ChatMessageStore.debugLog(() -> "[e33chat] Repost deduped | '" + repostStr + "'");
            return;
        }
        lastRepostText = repostStr;
        lastRepostTime = nowMs;
        ChatMessageStore.debugLog(() -> "[e33chat] Repost to vanilla | '" + repostStr + "' | quoting=" + quoting);
        e33chat$reposting = true;
        //#if MC >= 11900
        ((ChatHud) (Object) this).addMessage(reformatted, null, null);
        //#else
        //$$ ((ChatHud) (Object) this).addMessage(reformatted);
        //#endif
        e33chat$reposting = false;
    }

    // 替换图片占位码
    private void rewriteVanillaImageCode(Text finalComponent, CallbackInfo ci) {
        Text placeholder = BracketCodec.toPlaceholderText(finalComponent);
        if (placeholder == finalComponent) return;
        ci.cancel();
        e33chat$reposting = true;
        //#if MC >= 11900
        ((ChatHud) (Object) this).addMessage(placeholder, null, null);
        //#else
        //$$ ((ChatHud) (Object) this).addMessage(placeholder);
        //#endif
        e33chat$reposting = false;
    }

    // 捕获并入库消息
    private void captureMessage(Text finalComponent, CallbackInfo ci) {
        if (!ChatBubbleClientSetup.config().enabled()) return;
        if (e33chat$reposting) return;
        if (finalComponent == lastComponent) return;
        lastComponent = finalComponent;
        String text = finalComponent.getString();
        if (ChatMessageStore.consumeSuppressCapture()) {
            ci.cancel();
            Text name = ChatMessageStore.extractWhisperDisplayName(finalComponent,
                ChatMessageStore.ownDisplayName());
            ChatMessageStore.cacheOwnDecoratedName(name);
            ChatMessageStore.updateLatestOwnSenderName(name);
            ChatMessageStore.consumeSuppressQuoted();
            return;
        }
        SenderMeta meta = ChatMessageStore.consumePendingMeta();
        if (meta == null) {
            if (ChatMessageStore.isRecentDuplicate(text)) return;
            int lt = text.indexOf('<');
            boolean chatShape = lt >= 0 && text.indexOf('>', lt) > lt;
            if (chatShape && !com.niuqu.chatbubble.chat.capture.ChatClassifier.isJoinLeaveMessage(text)) {
                var cp = MinecraftClient.getInstance().player;
                var conn = cp != null ? cp.networkHandler : null;
                if (conn != null) {
                    SenderMeta parsed = com.niuqu.chatbubble.chat.capture.ChatPipeline.tryParsePlayerLine(
                        finalComponent, text, "CaptureFallback");
                    if (parsed != null) meta = parsed;
                }
            }
            if (meta == null) {
                meta = new SenderMeta(
                    new UUID(0, 0),
                    Text.translatable("e33chat.sender.system"),
                    finalComponent,
                    true,
                    null,
                    false, null
                );
            }
        }
        if (!meta.whisper() && !meta.isSystem()) {
            String sn = meta.senderName().getString();
            int wi = sn.indexOf("悄悄地对你说");
            if (wi > 0) {
                String partner = sn.substring(0, wi).trim();
                if (!partner.isEmpty()) {
                    Text decorated = ChatMessageStore.extractWhisperDisplayName(finalComponent,
                        Text.literal(partner));
                    String chatName = decorated.getString().isBlank() ? partner : decorated.getString();
                    meta = new SenderMeta(meta.senderUUID(), decorated,
                        meta.rawContent(), meta.isSystem(), chatName, true, chatName);
                }
            }
        }
        if (!meta.whisper()) {
            String partner = com.niuqu.chatbubble.chat.MessagePresentation.outgoingWhisperPartner(text);
            if (partner != null
                && com.niuqu.chatbubble.chat.MessagePresentation.hasWhisperKeywordBeforeColon(text)) {
                var self = MinecraftClient.getInstance().player;
                UUID ownId = self != null ? self.getUuid() : meta.senderUUID();
                String ownRaw = self != null ? self.getName().getString() : meta.rawPlayerName();
                Text content = meta.rawContent() != null && !meta.rawContent().getString().isBlank()
                    ? meta.rawContent()
                    : Text.literal(com.niuqu.chatbubble.chat.MessagePresentation.extractWhisperContent(text, partner));
                ChatMessageStore.debugLog(() -> "[e33chat] Outgoing whisper normalized | text='" + text
                    + "' | partner=" + partner + " | content='" + content.getString() + "'");
                meta = new SenderMeta(ownId, ChatMessageStore.ownDisplayName(),
                    content, false, ownRaw, true, partner);
            }
        }
        if (BlockList.isPlayerBlocked(meta.rawPlayerName(), meta.senderName(),
                ChatBubbleClientSetup.config().blockedPlayers())) {
            final String blockedName = meta.senderName().getString();
            ci.cancel();
            ChatMessageStore.debugLog(() -> "[e33chat] Blocked message dropped | sender='" + blockedName + "'");
            return;
        }
        EchoTracker.EchoMatch echo = ChatMessageStore.consumeEchoIfSenderMatches(meta.senderUUID(), meta.senderName(), text);
        if (echo.matched()) {
            if (meta.whisper()) {
                ci.cancel();
            } else if (echo.quoted()) {
                ci.cancel();
                repostToVanilla(meta.senderName(), ChatMessageStore.extractWhisperContent(text, meta), true);
            } else {
                rewriteVanillaImageCode(finalComponent, ci);
            }
            return;
        }
        if (ChatMessageStore.consumeEchoBySystemChat(text).matched()) {
            rewriteVanillaImageCode(finalComponent, ci);
            return;
        }
        if (meta.whisper()) {
            ci.cancel();
        }
        String rawStr = meta.rawContent().getString();
        String finalStr = finalComponent.getString();
        Text content;
        if (finalStr.contains(rawStr)) {
            content = meta.rawContent();
        } else if (!rawStr.isBlank()
                && !BracketCodec.parseOrExtract(meta.rawContent()).images().isEmpty()) {
            content = meta.rawContent();
        } else {
            content = finalComponent;
        }
        Text logComp = finalComponent, logContent = content;
        SenderMeta logMeta = meta;
        ChatMessageStore.debugLog(() -> "[e33chat] Capture | final='" + logComp.getString() + "' | content='" + logContent.getString() + "' | whisper=" + logMeta.whisper() + " | partner=" + logMeta.whisperPartner() + " | isSystem=" + logMeta.isSystem());
        if (!meta.whisper()) rewriteVanillaImageCode(finalComponent, ci);
        ChatMessageStore.addMessage(content, meta.senderUUID(), meta.senderName(), meta.isSystem(), meta.rawPlayerName(), meta.whisper(), meta.whisperPartner(), false);
    }
}
