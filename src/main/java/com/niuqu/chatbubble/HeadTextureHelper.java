package com.niuqu.chatbubble;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public final class HeadTextureHelper {
    private HeadTextureHelper() {}

    public static final Set<Identifier> BLENDED_HEAD_TEXTURES = new HashSet<>();

    public static NativeImage extractBlendedHead(NativeImage skin) {
        boolean isLegacy = skin.getWidth() / 2 == skin.getHeight();
        int xScale = skin.getWidth() / 64;
        int yScale = skin.getHeight() / (isLegacy ? 32 : 64);

        NativeImage head = new NativeImage(8 * xScale, 8 * yScale, false);

        for (int y = 0; y < head.getHeight(); y++) {
            for (int x = 0; x < head.getWidth(); x++) {
                //#if MC >= 12102
                int faceColor = skin.getColorArgb(8 * xScale + x, 8 * yScale + y);
                int hatColor = skin.getColorArgb(40 * xScale + x, 8 * yScale + y);
                head.setColorArgb(x, y, blendColors(faceColor, hatColor));
                //#else
                //#if MC >= 11800
                //$$ int faceColor = skin.getColor(8 * xScale + x, 8 * yScale + y);
                //$$ int hatColor = skin.getColor(40 * xScale + x, 8 * yScale + y);
                //$$ head.setColor(x, y, blendColors(faceColor, hatColor));
                //#else
                //$$ int faceColor = skin.getPixelColor(8 * xScale + x, 8 * yScale + y);
                //$$ int hatColor = skin.getPixelColor(40 * xScale + x, 8 * yScale + y);
                //$$ head.setPixelColor(x, y, blendColors(faceColor, hatColor));
                //#endif
                //#endif
            }
        }
        return head;
    }

    public static int blendColors(int color1, int color2) {
        float a1 = ((color1 >> 24) & 0xFF) / 255f;
        float r1 = ((color1 >> 16) & 0xFF) / 255f;
        float g1 = ((color1 >> 8) & 0xFF) / 255f;
        float b1 = (color1 & 0xFF) / 255f;

        float a2 = ((color2 >> 24) & 0xFF) / 255f;
        float r2 = ((color2 >> 16) & 0xFF) / 255f;
        float g2 = ((color2 >> 8) & 0xFF) / 255f;
        float b2 = (color2 & 0xFF) / 255f;

        float a3 = a2 * a2 + (1 - a2) * a1;
        float r3 = a2 * r2 + (1 - a2) * r1;
        float g3 = a2 * g2 + (1 - a2) * g1;
        float b3 = a2 * b2 + (1 - a2) * b1;

        return (Math.min((int)(a3 * 255f), 255) << 24)
             | (Math.min((int)(r3 * 255f), 255) << 16)
             | (Math.min((int)(g3 * 255f), 255) << 8)
             | Math.min((int)(b3 * 255f), 255);
    }

    public static Identifier getBlendedHeadLocation(Identifier skinLocation) {
        //#if MC >= 12100
        return Identifier.of("e33chat", skinLocation.getPath());
        //#else
        //$$ return new Identifier("e33chat", skinLocation.getPath());
        //#endif
    }

    public static void registerBlendedHead(Identifier skinLocation, NativeImage skinImage) {
        if (skinLocation == null || skinImage == null) return;
        if ("e33chat".equals(skinLocation.getNamespace())) return;
        if (!skinLocation.getPath().startsWith("skins/")) return;
        if (BLENDED_HEAD_TEXTURES.contains(skinLocation)) return;

        try {
            NativeImage blended = extractBlendedHead(skinImage);
            //#if MC >= 12105
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(getBlendedHeadLocation(skinLocation),
                            new NativeImageBackedTexture(() -> "Chat Head of " + skinLocation.getPath(), blended));
            //#else
            //$$ MinecraftClient.getInstance().getTextureManager()
            //$$         .registerTexture(getBlendedHeadLocation(skinLocation),
            //$$                 new NativeImageBackedTexture(blended));
            //#endif
            BLENDED_HEAD_TEXTURES.add(skinLocation);
        } catch (Exception ignored) {
        }
    }
}
