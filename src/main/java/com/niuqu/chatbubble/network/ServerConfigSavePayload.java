package com.niuqu.chatbubble.network;

import com.niuqu.chatbubble.config.ServerConfig;
import com.niuqu.chatbubble.config.ServerConfigManager;
import com.niuqu.chatbubble.chat.TemplateMatcher;
import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

//#if MC >= 12005
public record ServerConfigSavePayload(boolean useTpa, boolean historyEnabled, boolean templateDebug,
                                      boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
                                      boolean groupsEnabled,
                                      List<String> chatTemplates, List<String> whisperTemplates)
        implements CustomPayload {
//#else
//$$ public record ServerConfigSavePayload(boolean useTpa, boolean historyEnabled, boolean templateDebug,
//$$                                       boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
//$$                                       boolean groupsEnabled,
//$$                                       List<String> chatTemplates, List<String> whisperTemplates) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<ServerConfigSavePayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "server_config_save")
            //#else
            //$$ new Identifier("e33chat", "server_config_save")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "server_config_save");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, ServerConfigSavePayload> CODEC = PacketCodec.of(
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
            return new ServerConfigSavePayload(d.useTpa(), d.historyEnabled(), d.templateDebug(),
                d.mediaEnabled(), d.mediaAutoClean(), d.easyBotCompat(), d.groupsEnabled(),
                d.chatTemplates(), d.whisperTemplates());
        }
    );
    //#else
    //$$ public static ServerConfigSavePayload read(PacketByteBuf buf) {
    //$$     ServerConfigDto d = ServerConfigDto.decode(buf);
    //$$     return new ServerConfigSavePayload(d.useTpa(), d.historyEnabled(), d.templateDebug(),
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
    public Id<ServerConfigSavePayload> getId() { return ID; }
    //#endif

    public static void handleServer(ServerConfigSavePayload payload, ServerPlayerEntity player,
                                    java.util.function.Consumer<ServerConfig> applyAndSave) {
        Text error = validateTemplates(true, payload.chatTemplates());
        if (error == null) error = validateTemplates(false, payload.whisperTemplates());
        if (error != null) {
            player.sendMessage(Text.translatable("e33chat.server.save_failed", error)
                .formatted(Formatting.RED), false);
            return;
        }
        ServerConfig cfg = new ServerConfig();
        cfg.use_tpa = payload.useTpa();
        cfg.history_enabled = payload.historyEnabled();
        cfg.template_debug = payload.templateDebug();
        cfg.media_enabled = payload.mediaEnabled();
        cfg.media_auto_clean = payload.mediaAutoClean();
        cfg.easy_bot_compat = payload.easyBotCompat();
        cfg.groups_enabled = payload.groupsEnabled();
        cfg.chat_templates = new ArrayList<>(payload.chatTemplates());
        cfg.whisper_templates = new ArrayList<>(payload.whisperTemplates());
        applyAndSave.accept(cfg);
        player.sendMessage(Text.translatable("e33chat.server.saved"), false);
    }

    private static Text validateTemplates(boolean chat, List<String> templates) {
        for (int i = 0; i < templates.size(); i++) {
            TemplateMatcher.CompileResult result = TemplateMatcher.compile(templates.get(i));
            if (result.template() == null) {
                return Text.translatable("e33chat.server.template_invalid",
                    Text.translatable(chat ? "e33chat.server.kind_chat" : "e33chat.server.kind_whisper"),
                    i + 1, result.error());
            }
        }
        return null;
    }
}
