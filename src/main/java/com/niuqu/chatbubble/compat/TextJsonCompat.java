package com.niuqu.chatbubble.compat;

import net.minecraft.text.Text;

/**
 * Text ↔ JSON across the 1.21.6 codec rework.
 *
 * {@code Text.Serialization} was removed in 1.21.6; the JSON entry points now live
 * on {@code TextCodecs.CODEC} driven by JsonOps. The registry lookup argument is
 * only meaningful for the 1.21.0-1.21.5 API (registry-dependent hover contents);
 * both paths return null on failure so callers fall back to plain text.
 *
 * <p>The registry argument is typed {@code Object} on purpose:
 * {@code RegistryWrapper.WrapperLookup} does not exist before 1.19.3, and below
 * 1.21 nothing is serialised at all (both methods return null), so one signature
 * covers all 21 targets instead of a preprocessor fork in the public API.</p>
 */
public final class TextJsonCompat {
    private TextJsonCompat() {}

    public static String toJson(Text text, Object registries) {
        if (text == null) return null;
        //#if MC >= 12106
        try {
            return net.minecraft.text.TextCodecs.CODEC
                .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, text)
                .getOrThrow().toString();
        } catch (Throwable t) {
            return null;
        }
        //#else
        //#if MC >= 12100
        //$$ try {
        //$$     return net.minecraft.text.Text.Serialization.toJsonString(
        //$$         text, (net.minecraft.registry.RegistryWrapper.WrapperLookup) registries);
        //$$ } catch (Throwable t) { return null; }
        //#else
        //$$ return null;
        //#endif
        //#endif
    }

    public static Text fromJson(String json, Object registries) {
        if (json == null) return null;
        //#if MC >= 12106
        try {
            return net.minecraft.text.TextCodecs.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(json))
                .getOrThrow();
        } catch (Throwable t) {
            return null;
        }
        //#else
        //#if MC >= 12100
        //$$ try {
        //$$     return net.minecraft.text.Text.Serialization.fromJson(
        //$$         json, (net.minecraft.registry.RegistryWrapper.WrapperLookup) registries);
        //$$ } catch (Throwable t) { return null; }
        //#else
        //$$ return null;
        //#endif
        //#endif
    }
}
