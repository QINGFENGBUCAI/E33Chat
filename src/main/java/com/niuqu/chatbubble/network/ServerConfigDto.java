package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;

import java.util.List;

public record ServerConfigDto(boolean useTpa, boolean historyEnabled, boolean templateDebug,
                              boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
                              boolean groupsEnabled,
                              List<String> chatTemplates, List<String> whisperTemplates) {
    public static void encode(ServerConfigDto dto, PacketByteBuf buf) {
        buf.writeBoolean(dto.useTpa);
        buf.writeBoolean(dto.historyEnabled);
        buf.writeBoolean(dto.templateDebug);
        buf.writeBoolean(dto.mediaEnabled);
        buf.writeBoolean(dto.mediaAutoClean);
        buf.writeBoolean(dto.easyBotCompat);
        buf.writeBoolean(dto.groupsEnabled);
        ConfigSyncV2Payload.writeList(buf, dto.chatTemplates);
        ConfigSyncV2Payload.writeList(buf, dto.whisperTemplates);
    }

    public static ServerConfigDto decode(PacketByteBuf buf) {
        boolean useTpa = buf.readBoolean();
        boolean history = buf.readBoolean();
        boolean debug = buf.readBoolean();
        boolean media = buf.readBoolean();
        boolean autoClean = buf.readBoolean();
        boolean easyBot = buf.readBoolean();
        boolean groups = buf.readBoolean();
        List<String> chat = ConfigSyncV2Payload.readList(buf);
        List<String> whisper = ConfigSyncV2Payload.readList(buf);
        return new ServerConfigDto(useTpa, history, debug, media, autoClean, easyBot, groups, chat, whisper);
    }
}
