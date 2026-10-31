package com.niuqu.chatbubble.chat.notification;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.store.ChatMessageStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import java.util.UUID;

// 聊天消息效果
public class ChatMessageEffects implements ChatMessageStore.MessageEffectObserver {
    // 处理提及或引用
    @Override
    public void onMentionOrQuote(Text content, ChatMessageStore.SenderMeta meta,
                                 int index, String replySender) {
        MentionNotificationController.INSTANCE.onMessageCaptured(content, meta, index, replySender);
    }

    // 处理收到私聊
    @Override
    public void onWhisperReceived(UUID senderUUID, Text senderName, Text content, int index) {
        MentionNotificationController.INSTANCE.onWhisperReceived(senderUUID, senderName, content, index);
    }

    // 处理系统消息
    @Override
    public void onSystemMessage(Text content, int index) {
        MentionNotificationController.INSTANCE.onSystemMessage(content, index);
    }

    // 播放公共消息音效
    @Override
    public void onPublicChatSound() {
        playChime();
    }

    // 播放引用消息音效
    @Override
    public void onQuoteSound() {
        playChime();
    }

    // 播放提示音
    private static void playChime() {
        NotificationSoundGate.tryPlay(() -> {
            var player = MinecraftClient.getInstance().player;
            if (player != null) {
                //#if MC >= 26000
                try {
                    net.minecraft.sound.SoundEvent sound = SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
                    if (sound != null) {
                        player.playSound(sound, 0.6F * ChatBubbleClientSetup.config().soundVolume() / 100f, 1.0F);
                    }
                } catch (Exception e) {
                    ChatMessageStore.debugLog(() -> "[e33chat] Sound play failed: " + e.getMessage());
                }
                //#else
                //$$ player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.6F * ChatBubbleClientSetup.config().soundVolume() / 100f, 1.0F);
                //#endif
            }
        });
    }
}
