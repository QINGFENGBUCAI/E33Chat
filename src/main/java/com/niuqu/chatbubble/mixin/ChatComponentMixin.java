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

@Mixin(value = ChatHud.class, priority = 500)
public class ChatComponentMixin {
    private Text lastComponent;
    private boolean e33chat$shifted;
    private boolean e33chat$reposting;
    private String lastRepostText;
    private long lastRepostTime;

    //#if MC >= 26000

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        if (ChatBubbleClientSetup.config().enabled()
                && MinecraftClient.getInstance().currentScreen instanceof ChatBubbleScreen) {
            ci.cancel();
        }
    }
    //#else
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

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, int tickDelta, int mouseX, int mouseY,
                                boolean focused, CallbackInfo ci) {
        if (e33chat$shifted) {
            context.getMatrices().pop();
        }
    }
    //#endif

    //#if MC >= 26000

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, net.minecraft.network.chat.MessageSignature signature,
                              net.minecraft.client.multiplayer.chat.GuiMessageSource source,
                              net.minecraft.client.multiplayer.chat.GuiMessageTag tag, CallbackInfo ci) {
        captureMessage(message, ci);
    }
    //#else
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        captureMessage(message, ci);
    }
    //#endif

    //#if MC >= 11900
    //#if MC < 26000
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void onAddMessageFull(Text message, MessageSignatureData signature,
                                  MessageIndicator indicator, CallbackInfo ci) {
        captureMessage(message, ci);
    }
    //#endif
    //#endif

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
            repostToVanilla(name, ChatMessageStore.extractWhisperContent(text, null),
                ChatMessageStore.consumeSuppressQuoted());
            return;
        }

        SenderMeta meta = ChatMessageStore.consumePendingMeta();
        if (meta == null) {
            if (ChatMessageStore.isRecentDuplicate(text)) return;
            meta = new SenderMeta(
                new UUID(0, 0),
                Text.translatable("e33chat.sender.system"),
                finalComponent,
                true,
                null,
                false, null
            );
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
            if (meta.whisper() || echo.quoted()) {
                ci.cancel();
                repostToVanilla(meta.senderName(), ChatMessageStore.extractWhisperContent(text, meta), echo.quoted());
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
            repostToVanilla(meta.senderName(), ChatMessageStore.extractWhisperContent(text, meta), false);
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
        rewriteVanillaImageCode(finalComponent, ci);
        ChatMessageStore.addMessage(content, meta.senderUUID(), meta.senderName(), meta.isSystem(), meta.rawPlayerName(), meta.whisper(), meta.whisperPartner(), false);
    }
}
