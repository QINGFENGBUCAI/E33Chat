package com.niuqu.chatbubble.network;

import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

//#if MC >= 12005
public record ServerConfigScreenPayload(boolean useTpa, boolean historyEnabled, boolean templateDebug,
                                        boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
                                        boolean groupsEnabled,
                                        List<String> chatTemplates, List<String> whisperTemplates)
        implements CustomPayload {
//#else
//$$ public record ServerConfigScreenPayload(boolean useTpa, boolean historyEnabled, boolean templateDebug,
//$$                                          boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
//$$                                          boolean groupsEnabled,
//$$                                          List<String> chatTemplates, List<String> whisperTemplates) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<ServerConfigScreenPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "server_config_screen")
            //#else
            //$$ new Identifier("e33chat", "server_config_screen")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "server_config_screen");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, ServerConfigScreenPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> ServerConfigDto.encode(new ServerConfigDto(
            value.useTpa, value.historyEnabled, value.templateDebug, value.mediaEnabled,
            value.mediaAutoClean, value.easyBotCompat, value.groupsEnabled, value.chatTemplates, value.whisperTemplates), buf),
        //#else
        //$$ (value, buf) -> ServerConfigDto.encode(new ServerConfigDto(
        //$$     value.useTpa, value.historyEnabled, value.templateDebug, value.mediaEnabled,
        //$$     value.mediaAutoClean, value.easyBotCompat, value.groupsEnabled, value.chatTemplates, value.whisperTemplates), buf),
        //#endif
        buf -> {
            ServerConfigDto d = ServerConfigDto.decode(buf);
            return new ServerConfigScreenPayload(d.useTpa(), d.historyEnabled(), d.templateDebug(),
                d.mediaEnabled(), d.mediaAutoClean(), d.easyBotCompat(), d.groupsEnabled(),
                d.chatTemplates(), d.whisperTemplates());
        }
    );
    //#else
    //$$ public static ServerConfigScreenPayload read(PacketByteBuf buf) {
    //$$     ServerConfigDto d = ServerConfigDto.decode(buf);
    //$$     return new ServerConfigScreenPayload(d.useTpa(), d.historyEnabled(), d.templateDebug(),
    //$$         d.mediaEnabled(), d.mediaAutoClean(), d.easyBotCompat(), d.groupsEnabled(),
    //$$         d.chatTemplates(), d.whisperTemplates());
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     ServerConfigDto.encode(new ServerConfigDto(
    //$$         useTpa, historyEnabled, templateDebug, mediaEnabled,
    //$$         mediaAutoClean, easyBotCompat, groupsEnabled, chatTemplates, whisperTemplates), buf);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public Id<ServerConfigScreenPayload> getId() { return ID; }
    //#endif
}
