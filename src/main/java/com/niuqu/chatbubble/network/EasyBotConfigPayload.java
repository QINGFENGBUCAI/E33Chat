package com.niuqu.chatbubble.network;

import com.niuqu.chatbubble.store.ChatMessageStore;
import net.minecraft.network.PacketByteBuf;
//#if MC >= 12005
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
//#endif
import net.minecraft.util.Identifier;

//#if MC >= 12005
public record EasyBotConfigPayload(boolean easyBotCompat) implements CustomPayload {
//#else
//$$ public record EasyBotConfigPayload(boolean easyBotCompat) {
//#endif

    //#if MC >= 12005
    public static final CustomPayload.Id<EasyBotConfigPayload> ID =
        new CustomPayload.Id<>(
            //#if MC >= 12000
            Identifier.of("e33chat", "config_sync_easybot")
            //#else
            //$$ new Identifier("e33chat", "config_sync_easybot")
            //#endif
        );
    //#else
    //$$ public static final Identifier ID = new Identifier("e33chat", "config_sync_easybot");
    //#endif

    //#if MC >= 12005
    public static final PacketCodec<PacketByteBuf, EasyBotConfigPayload> CODEC = PacketCodec.of(
        //#if MC >= 26000
        (buf, value) -> buf.writeBoolean(value.easyBotCompat),
        //#else
        //$$ (value, buf) -> buf.writeBoolean(value.easyBotCompat),
        //#endif
        buf -> new EasyBotConfigPayload(buf.readBoolean())
    );
    //#else
    //$$ public static EasyBotConfigPayload read(PacketByteBuf buf) {
    //$$     return new EasyBotConfigPayload(buf.readBoolean());
    //$$ }
    //$$ public PacketByteBuf write(PacketByteBuf buf) {
    //$$     buf.writeBoolean(easyBotCompat);
    //$$     return buf;
    //$$ }
    //#endif

    //#if MC >= 12005
    @Override
    public Id<EasyBotConfigPayload> getId() { return ID; }
    //#endif

    public static void handle(EasyBotConfigPayload payload) {
        ChatMessageStore.setEasyBotCompat(payload.easyBotCompat());
    }
}
