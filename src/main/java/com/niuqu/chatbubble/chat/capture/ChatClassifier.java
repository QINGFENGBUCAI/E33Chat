package com.niuqu.chatbubble.chat.capture;

import com.niuqu.chatbubble.GuiCompat;
import com.niuqu.chatbubble.config.ChatBubbleConfig;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

public final class ChatClassifier {
    private ChatClassifier() {}

    public static String[] nameCandidates(PlayerListEntry info) {
        var out = new java.util.LinkedHashSet<String>();
        String profile = GuiCompat.profileName(info.getProfile());
        addNameVariants(out, profile);
        var tab = info.getDisplayName();
        if (tab != null) addNameVariants(out, tab.getString().trim());
        return out.toArray(new String[0]);
    }

    public static void addNameVariants(java.util.Set<String> out, String name) {
        if (name == null || name.isEmpty()) return;
        out.add(name);
        String stripped = name.replaceAll("§.", "");
        if (!stripped.isEmpty()) out.add(stripped);
    }

    public static boolean isVanillaBroadcast(Text message) {
        if (message.getContent() instanceof net.minecraft.text.TranslatableTextContent tc) {
            String key = tc.getKey();
            return key.startsWith("chat.type.advancement.")
                || key.startsWith("death.")
                || key.startsWith("multiplayer.player.")
                || key.startsWith("commands.")
                || key.equals("chat.type.admin")
                || key.equals("chat.type.announcement")
                || key.equals("chat.type.emote")
                || key.startsWith("chat.type.team.");
        }
        return false;
    }

    public static Text argAsComponent(Object arg) {
        return arg instanceof Text c ? c : Text.literal(String.valueOf(arg));
    }

    public static PlayerListEntry resolveOnlinePlayer(String displayName) {
        var player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null || displayName.isEmpty()) return null;
        var online = player.networkHandler.getPlayerList();
        for (var info : online) {
            for (String cand : nameCandidates(info)) {
                if (cand.equals(displayName)) return info;
            }
        }

        PlayerListEntry best = null;
        int bestLen = 0;
        for (var info : online) {
            for (String cand : nameCandidates(info)) {
                if (cand.length() >= 3 && cand.length() > bestLen && displayName.contains(cand)) {
                    best = info;
                    bestLen = cand.length();
                }
            }
        }
        return best;
    }

    public static boolean classifyByKey(Text message) {
        if (!(message.getContent() instanceof net.minecraft.text.TranslatableTextContent tc)) return false;
        String key = tc.getKey();
        Object[] args = tc.getArgs();

        if (key.equals("commands.message.display.incoming") && args.length >= 2) {
            Text name = argAsComponent(args[0]);
            Text content = argAsComponent(args[1]);
            String displayName = name.getString().replaceAll("§.", "").trim();
            var info = resolveOnlinePlayer(displayName);
            String profile = info != null ? GuiCompat.profileName(info.getProfile()) : displayName;
            UUID uuid = info != null ? GuiCompat.profileId(info.getProfile()) : new UUID(0, 0);
            ChatMessageStore.debugLog(() -> "[e33chat] Key(whisper in) | name=" + profile + " | content='" + content.getString() + "'");
            ChatMessageStore.setPendingMeta(new ChatMessageStore.SenderMeta(uuid, name, content, false, profile, true, profile));
            return true;
        }

        if (key.equals("commands.message.display.outgoing")) {
            if (ChatMessageStore.hasPendingWhisperEcho()) {
                ChatMessageStore.consumeWhisperEcho();
                ChatMessageStore.markSuppressCapture();
                ChatMessageStore.debugLog(() -> "[e33chat] Key(whisper echo suppressed)");
                return true;
            }
            var player = MinecraftClient.getInstance().player;
            if (player != null && args.length >= 2) {
                String partner = argAsComponent(args[0]).getString().replaceAll("§.", "").trim();
                Text content = argAsComponent(args[1]);
                String own = player.getName().getString();
                ChatMessageStore.debugLog(() -> "[e33chat] Key(whisper out) | partner=" + partner + " | content='" + content.getString() + "'");
                ChatMessageStore.setPendingMeta(new ChatMessageStore.SenderMeta(player.getUuid(),
                    Text.literal(own), content, false, own, true, partner));
                return true;
            }
            return false;
        }

        if (key.equals("chat.type.text") && args.length >= 2) {
            Text name = argAsComponent(args[0]);
            Text content = argAsComponent(args[1]);
            String contentStr = content.getString();

            if (contentStr.startsWith("xaero-waypoint:")
                || contentStr.startsWith("xaero_waypoint:")
                || contentStr.startsWith("xaero_waypoint_add:")) {
                ChatMessageStore.debugLog(() -> "[e33chat] Key(waypoint data) -> system");
                ChatMessageStore.setPendingMeta(new ChatMessageStore.SenderMeta(new UUID(0, 0),
                    Text.translatable("e33chat.sender.system"), message, true, null, false, null));
                return true;
            }
            String displayName = name.getString().replaceAll("§.", "").trim();
            var info = resolveOnlinePlayer(displayName);
            String profile;
            UUID uuid;
            if (info != null) {
                profile = GuiCompat.profileName(info.getProfile());
                uuid = GuiCompat.profileId(info.getProfile());
            } else {
                UUID su = ChatMessageStore.findSeenUuid(displayName);
                if (su != null) {
                    profile = displayName;
                    uuid = su;
                } else {
                    profile = displayName;
                    uuid = new UUID(0, 0);
                }
            }
            ChatMessageStore.debugLog(() -> "[e33chat] Key(chat) | name=" + profile + " | display='" + name.getString() + "' | content='" + content.getString() + "'");
            ChatMessageStore.setPendingMeta(new ChatMessageStore.SenderMeta(uuid, name, content, false, profile, false, null));
            return true;
        }

        if (isVanillaBroadcast(message)) {
            boolean isSystem = com.niuqu.chatbubble.ChatBubbleClientSetup.config() == null || !com.niuqu.chatbubble.ChatBubbleClientSetup.config().systemChatAsBubble();
            ChatMessageStore.debugLog(() -> "[e33chat] Key(broadcast) | key=" + key);
            ChatMessageStore.setPendingMeta(new ChatMessageStore.SenderMeta(new UUID(0, 0),
                Text.translatable("e33chat.sender.system"), message, isSystem, null, false, null));
            return true;
        }

        return false;
    }
}
