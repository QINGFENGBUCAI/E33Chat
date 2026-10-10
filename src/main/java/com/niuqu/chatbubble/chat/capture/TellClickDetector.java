package com.niuqu.chatbubble.chat.capture;

import com.niuqu.chatbubble.GuiCompat;
import com.niuqu.chatbubble.chat.MessagePresentation;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class TellClickDetector {
    private TellClickDetector() {}

    public static ChatMessageStore.SenderMeta detectByTellClick(Text message, String text) {
        if (ChatClassifier.isVanillaBroadcast(message)) return null;
        var player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null) return null;
        final int[] pos = {0};
        final int[] range = {-1, -1};
        final String[] tellName = {null};
        final String[] clickedText = {null};
        message.visit((style, str) -> {
            int s = pos[0], e = s + str.length();
            pos[0] = e;
            var click = style.getClickEvent();
            if (tellName[0] == null && click != null
                && click.getAction() == net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND) {
                String cmd = com.niuqu.chatbubble.compat.StyleCompat.clickValue(click);
                for (String p : new String[]{"/tell ", "/msg ", "/w ", "/whisper "}) {
                    if (cmd != null && cmd.startsWith(p)) {
                        String n = cmd.substring(p.length()).trim();
                        int sp = n.indexOf(' ');
                        if (sp > 0) n = n.substring(0, sp);
                        if (!n.isEmpty()) {
                            tellName[0] = n;
                            range[0] = s;
                            range[1] = e;
                            clickedText[0] = str;
                        }
                        break;
                    }
                }
            }
            return java.util.Optional.<Object>empty();
        }, net.minecraft.text.Style.EMPTY);
        int nameRangeLimit = Math.max(32, text.length() / 3);
        if (tellName[0] == null || range[0] > nameRangeLimit) return null;

        net.minecraft.client.network.PlayerListEntry sender = null;
        for (var info : player.networkHandler.getPlayerList()) {
            String profile = GuiCompat.profileName(info.getProfile());
            if (profile.equals(tellName[0]) || profile.replaceAll("§.", "").equals(tellName[0])) {
                sender = info;
                break;
            }
        }
        UUID cachedId = null;
        if (sender == null) {
            cachedId = ChatMessageStore.findSeenUuid(tellName[0]);
            if (cachedId == null) return null;
        }

        if (sender != null) {
            String clicked = clickedText[0].replaceAll("§.", "").trim();
            boolean clickedIsName = false;
            for (String cand : ChatClassifier.nameCandidates(sender)) {
                if (!cand.isEmpty() && clicked.contains(cand)) { clickedIsName = true; break; }
            }
            if (!clickedIsName) return null;
        }

        int b = range[1];
        if (b < text.length() && text.charAt(b) == '>') b++;
        int contentStart = MessagePresentation.skipSeparators(text, b);
        if (contentStart >= text.length()) return null;

        String profile = sender != null ? GuiCompat.profileName(sender.getProfile()) : tellName[0];
        UUID id = sender != null ? GuiCompat.profileId(sender.getProfile()) : cachedId;
        Text displayName = ChatPipeline.cleanNameArea(message, 0, b, tellName[0], Text.literal(profile));
        Text content = ChatMessageStore.sliceStyled(message, contentStart, text.length());
        ChatMessageStore.debugLog(() -> "[e33chat] System(tell click) | text='" + text + "' | name=" + profile + " | display='" + displayName.getString() + "' | content='" + content.getString() + "'");
        return new ChatMessageStore.SenderMeta(
            id,
            displayName,
            content,
            false,
            profile,
            false, null
        );
    }
}
