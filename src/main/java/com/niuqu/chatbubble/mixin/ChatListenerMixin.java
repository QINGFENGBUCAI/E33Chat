package com.niuqu.chatbubble.mixin;

//#if MC >= 11900
import com.mojang.authlib.GameProfile;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.store.ChatMessageStore.SenderMeta;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

// 捕获并路由聊天消息
@Mixin(value = net.minecraft.client.network.message.MessageHandler.class, priority = 500)
public class ChatListenerMixin {
    // 提取装饰名
    private static Text extractDecoratedName(Text fullLine, String contentStr,
                                                  String rawName, Text fallback) {
        return com.niuqu.chatbubble.chat.capture.ChatPipeline.extractDecoratedName(fullLine, contentStr, rawName, fallback);
    }

    // 判断进出消息
    private static boolean isJoinLeaveMessage(String text) {
        return com.niuqu.chatbubble.chat.capture.ChatClassifier.isJoinLeaveMessage(text);
    }

    // 按类型键分类
    private static boolean classifyByKey(Text message) {
        return com.niuqu.chatbubble.chat.capture.ChatClassifier.classifyByKey(message);
    }

    // 检测私聊点击
    private static SenderMeta detectByTellClick(Text message, String text) {
        return com.niuqu.chatbubble.chat.capture.TellClickDetector.detectByTellClick(message, text);
    }

    // 检测系统私聊（保留样式）
    private static SenderMeta detectWhisperInSystemMessage(Text message, String text, String logTag) {
        return com.niuqu.chatbubble.chat.capture.WhisperDetector.detectWhisperInSystemMessage(message, text, logTag);
    }

    // 按模板匹配
    private static SenderMeta matchByTemplate(Text message, String text) {
        return com.niuqu.chatbubble.chat.capture.TemplateLayer.matchByTemplate(message, text);
    }

    // 按模板匹配带日志
    private static SenderMeta matchByTemplate(Text message, String text, String logTag) {
        return com.niuqu.chatbubble.chat.capture.TemplateLayer.matchByTemplate(message, text, logTag);
    }

    //#if MC < 11903
    //$$ // 1.19.0-1.19.2: onChatMessage 没有 GameProfile 参数（1.19.3 才加入），
    //$$ // 且 onProfilelessMessage 尚不存在。这里降级：把装饰后的行交给与系统
    //$$ // 通道同一套文本启发式解析，识别不了的落灰字系统样式。
    //$$ @Inject(method = "onChatMessage", at = @At("HEAD"))
    //$$ private void onPlayerChat(SignedMessage message, MessageType.Parameters params, CallbackInfo ci) {
    //$$     Text raw = message.getContent();
    //$$     String rawStr = raw.getString();
    //$$     if (rawStr.startsWith("xaero-waypoint:")
    //$$             || rawStr.startsWith("xaero_waypoint:")
    //$$             || rawStr.startsWith("xaero_waypoint_add:")) {
    //$$         return;
    //$$     }
    //$$     var connection = MinecraftClient.getInstance().player != null
    //$$             ? MinecraftClient.getInstance().player.networkHandler : null;
    //$$     if (connection != null) {
    //$$         SenderMeta parsed = com.niuqu.chatbubble.chat.capture.ChatPipeline.tryParsePlayerLine(raw, rawStr, "Signed1192");
    //$$         if (parsed != null) { ChatMessageStore.setPendingMeta(parsed); return; }
    //$$     }
    //$$     ChatMessageStore.setPendingMeta(new SenderMeta(
    //$$         new UUID(0, 0), Text.translatable("e33chat.sender.system"), raw, true, null, false, null));
    //$$ }
    //#else
    // 处理玩家聊天
    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onPlayerChat(SignedMessage message, GameProfile gameProfile,
                               MessageType.Parameters params, CallbackInfo ci) {
        UUID senderId = gameProfile.getId();
        //#if MC >= 26000
        Text raw = message.decoratedContent();
        //#else
        //$$ Text raw = message.getContent();
        //#endif
        String rawStr = raw.getString();
        if (rawStr.startsWith("xaero-waypoint:")
            || rawStr.startsWith("xaero_waypoint:")
            || rawStr.startsWith("xaero_waypoint_add:")) {
            return;
        }
        String name = gameProfile.getName();
        Text decoratedText = params.applyChatDecoration(raw);
        String decoratedStr = decoratedText.getString();
        boolean isWhisper = false;
        boolean isOutgoing = false;
        String whisperPartner = null;
        //#if MC >= 12005
        if (params.type().matchesKey(MessageType.MSG_COMMAND_INCOMING)) {
            isWhisper = true;
            whisperPartner = name;
        } else if (params.type().matchesKey(MessageType.MSG_COMMAND_OUTGOING)) {
            isWhisper = true;
            isOutgoing = true;
            whisperPartner = params.targetName().map(Text::getString).orElse(null);
        }
        //#else
        //$$ // 1.20.4 and earlier: no MSG_COMMAND_* MessageType, rely on keyword-based whisper detection
        //#endif
        if (!isWhisper && com.niuqu.chatbubble.chat.MessagePresentation.hasWhisperKeywordBeforeColon(decoratedStr)) {
            isWhisper = true;
            var self = MinecraftClient.getInstance().player;
            String decoratedLower = decoratedStr.toLowerCase(java.util.Locale.ROOT);
            if (decoratedStr.contains("对你说") || decoratedLower.contains("whispers to you")) {
                if (self != null) whisperPartner = name;
            } else {
                String outgoingPartner =
                    com.niuqu.chatbubble.chat.MessagePresentation.outgoingWhisperPartner(decoratedStr);
                if (outgoingPartner != null) {
                    whisperPartner = outgoingPartner;
                    isOutgoing = true;
                }
            }
        }
        String pattern = "<" + name + "> ";
        int idx = rawStr.indexOf(pattern);
        int contentStart = idx >= 0 ? idx + pattern.length() : -1;
        int prefixEnd = idx;
        if (contentStart < 0) {
            int i2 = rawStr.indexOf(name + "> ");
            if (i2 > 0) {
                int open = rawStr.lastIndexOf('<', i2);
                if (open >= 0 && rawStr.indexOf('>', open) == i2 + name.length()) {
                    contentStart = i2 + name.length() + 2;
                    prefixEnd = open;
                }
            }
        }
        if (contentStart >= 0) {
            String cleanContent = rawStr.substring(contentStart);
            Text displayName = extractDecoratedName(raw, cleanContent, name,
                Text.literal((rawStr.substring(0, prefixEnd) + name).trim()));
            Text contentComp = ChatMessageStore.sliceStyled(raw, contentStart, rawStr.length());
            ChatMessageStore.setPendingMeta(new SenderMeta(
                senderId != null ? senderId : new UUID(0, 0),
                displayName, contentComp, false, name, isWhisper, whisperPartner));
            return;
        }
        Text playerContent = raw;
        Text senderName = Text.literal(name);
        if (isWhisper) {
            playerContent = Text.literal(com.niuqu.chatbubble.chat.MessagePresentation.extractWhisperContent(rawStr, name));
            Text fallback = isOutgoing ? ChatMessageStore.ownDisplayNameOr(params.name()) : senderName;
            senderName = ChatMessageStore.extractWhisperDisplayName(decoratedText, fallback);
        } else {
            senderName = extractDecoratedName(decoratedText, rawStr, name, senderName);
        }
        var self = MinecraftClient.getInstance().player;
        if (senderId != null && self != null && senderId.equals(self.getUuid())) {
            ChatMessageStore.cacheOwnDecoratedName(senderName);
        }
        ChatMessageStore.debugLog("[e33chat] PlayerChat | raw='" + rawStr + "' | sender='" + senderName.getString() + "' | content='" + playerContent.getString() + "'");
        ChatMessageStore.setPendingMeta(new SenderMeta(
            senderId != null ? senderId : new UUID(0, 0),
            senderName, playerContent, false, name, isWhisper, whisperPartner));
    }
    //#endif
    //#if MC >= 11903
    // 处理伪装聊天
    @Inject(method = "onProfilelessMessage", at = @At("HEAD"))
    private void onDisguisedChat(Text message, MessageType.Parameters params, CallbackInfo ci) {
        String msgStr = message.getString();
        if (msgStr.startsWith("xaero-waypoint:")
            || msgStr.startsWith("xaero_waypoint:")
            || msgStr.startsWith("xaero_waypoint_add:")) {
            return;
        }
        boolean hasSender = params.name() != null
            && !params.name().getString().replaceAll("§.", "").trim().isEmpty();
        Text decoratedLine = params.applyChatDecoration(message);
        String decoratedStr = decoratedLine.getString();
        boolean isWhisper = false;
        boolean isOutgoing = false;
        String whisperPartner = null;
        //#if MC >= 12005
        if (params.type().matchesKey(MessageType.MSG_COMMAND_INCOMING)) {
            isWhisper = true;
            whisperPartner = hasSender ? params.name().getString() : null;
        } else if (params.type().matchesKey(MessageType.MSG_COMMAND_OUTGOING)) {
            isWhisper = true;
            isOutgoing = true;
            whisperPartner = params.targetName().map(Text::getString).orElse(null);
        }
        //#else
        //$$ // 1.20.4 and earlier: no MSG_COMMAND_* MessageType, rely on keyword-based whisper detection
        //#endif
        if (!isWhisper) {
            SenderMeta wm = detectWhisperInSystemMessage(decoratedLine, decoratedStr, "disguised");
            if (wm != null) { ChatMessageStore.setPendingMeta(wm); return; }
        }
        if (hasSender) {
            Text disContent = message;
            Text disSender = params.name();
            if (isWhisper) {
                disContent = Text.literal(com.niuqu.chatbubble.chat.MessagePresentation.extractWhisperContent(msgStr, params.name().getString()));
                Text fallback = isOutgoing ? ChatMessageStore.ownDisplayNameOr(params.name()) : disSender;
                disSender = ChatMessageStore.extractWhisperDisplayName(decoratedLine, fallback);
            } else {
                disSender = extractDecoratedName(decoratedLine, msgStr, params.name().getString(), disSender);
            }
            ChatMessageStore.debugLog("[e33chat] Disguised | raw='" + msgStr + "' | whisper=" + isWhisper + " | partner=" + whisperPartner + " | sender='" + disSender.getString() + "' | content='" + disContent.getString() + "'");
            ChatMessageStore.setPendingMeta(new SenderMeta(
                new UUID(0, 0), disSender, disContent, false,
                params.name().getString(), isWhisper, whisperPartner));
            return;
        }
        if (!isOutgoing
            && (!ChatMessageStore.serverChatTemplates().isEmpty()
                || !ChatMessageStore.serverWhisperTemplates().isEmpty())) {
            SenderMeta tpl = matchByTemplate(message, msgStr, "Disguised");
            if (tpl != null) { ChatMessageStore.setPendingMeta(tpl); return; }
        }
        var connection = MinecraftClient.getInstance().player != null
            ? MinecraftClient.getInstance().player.networkHandler : null;
        SenderMeta tc = detectByTellClick(message, msgStr);
        if (tc != null) { ChatMessageStore.setPendingMeta(tc); return; }
        if (!isWhisper && isJoinLeaveMessage(msgStr)) {
            ChatMessageStore.debugLog(() -> "[e33chat] Disguised(join/leave -> system) | text='" + msgStr + "'");
            var cfg = ChatBubbleClientSetup.config();
            boolean isSystem = cfg == null || !cfg.systemChatAsBubble();
            ChatMessageStore.setPendingMeta(new SenderMeta(new UUID(0, 0),
                Text.translatable("e33chat.sender.system"), message, isSystem, null, false, null));
            return;
        }
        if (connection != null && !isWhisper) {
            SenderMeta parsed = com.niuqu.chatbubble.chat.capture.ChatPipeline.tryParsePlayerLine(message, msgStr, "Disguised");
            if (parsed != null) { ChatMessageStore.setPendingMeta(parsed); return; }
        }
        ChatMessageStore.debugLog(() -> "[e33chat] Disguised(guard fallback -> gray) | text='" + msgStr + "'");
        var cfg = ChatBubbleClientSetup.config();
        boolean isSystem = cfg == null || !cfg.systemChatAsBubble();
        ChatMessageStore.debugLog(() -> "[e33chat] systemChatAsBubble config=" + (cfg != null ? cfg.systemChatAsBubble() : "null") + " | isSystem=" + isSystem);
        ChatMessageStore.setPendingMeta(new SenderMeta(
            new UUID(0, 0), Text.translatable("e33chat.sender.system"),
            message, isSystem, null, false, null));
    }
    //#endif

    // 处理系统消息
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onSystemChat(Text message, boolean overlay, CallbackInfo ci) {
        //#if MC >= 26000
        //#else
        if (overlay) return;
        //#endif
        if (classifyByKey(message)) return;
        String sysText = message.getString();
        if (com.niuqu.chatbubble.chat.capture.EchoSuppressor.trySuppressOutgoingEcho(sysText)) return;
        ChatMessageStore.debugLog(() -> "[e33chat] System | text='" + sysText + "' | overlay=" + overlay);
        String text = message.getString();
        var connection = MinecraftClient.getInstance().player != null
            ? MinecraftClient.getInstance().player.networkHandler : null;
        if ((!ChatMessageStore.serverChatTemplates().isEmpty()
                || !ChatMessageStore.serverWhisperTemplates().isEmpty()) && connection != null) {
            SenderMeta tpl = matchByTemplate(message, text);
            if (tpl != null) { ChatMessageStore.setPendingMeta(tpl); return; }
        }
        if (ChatMessageStore.isEasyBotCompat()) {
            SenderMeta eb = com.niuqu.chatbubble.chat.capture.EasyBotParser.tryParse(message, text);
            if (eb != null) {
                ChatMessageStore.debugLog(() -> "[e33chat] System(EasyBot) | name='" + eb.senderName().getString()
                    + "' | content='" + eb.rawContent().getString() + "'");
                ChatMessageStore.setPendingMeta(eb);
                return;
            }
            int open = text.indexOf('<');
            if (open >= 0 && text.indexOf('>', open) > open) {
                ChatMessageStore.debugLog(() -> "[e33chat] System(EasyBot miss) | text='" + text + "'");
            }
        }
        SenderMeta wm = detectWhisperInSystemMessage(message, text, "whisper");
        if (wm != null) { ChatMessageStore.setPendingMeta(wm); return; }
        SenderMeta tc = detectByTellClick(message, text);
        if (tc != null) { ChatMessageStore.setPendingMeta(tc); return; }
        if (isJoinLeaveMessage(text)) {
            ChatMessageStore.debugLog(() -> "[e33chat] System(join/leave -> system) | text='" + text + "'");
            var cfg = ChatBubbleClientSetup.config();
            boolean isSystem = cfg == null || !cfg.systemChatAsBubble();
            ChatMessageStore.setPendingMeta(new SenderMeta(new UUID(0, 0),
                Text.translatable("e33chat.sender.system"), message, isSystem, null, false, null));
            return;
        }
        if (connection != null) {
            SenderMeta parsed = com.niuqu.chatbubble.chat.capture.ChatPipeline.tryParsePlayerLine(message, text, "System");
            if (parsed != null) { ChatMessageStore.setPendingMeta(parsed); return; }
        }
        ChatMessageStore.debugLog(() -> "[e33chat] System(guard fallback -> gray) | text='" + text + "'");
        var cfg = ChatBubbleClientSetup.config();
        boolean isSystem = cfg == null || !cfg.systemChatAsBubble();
        ChatMessageStore.debugLog(() -> "[e33chat] systemChatAsBubble config=" + (cfg != null ? cfg.systemChatAsBubble() : "null") + " | isSystem=" + isSystem);
        ChatMessageStore.setPendingMeta(new SenderMeta(
            new UUID(0, 0), Text.translatable("e33chat.sender.system"),
            message, isSystem, null, false, null));
    }
}
//#else
//$$ public class ChatListenerMixin {
//$$ }
//#endif
