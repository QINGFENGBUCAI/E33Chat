package com.niuqu.chatbubble.compat;

import com.niuqu.chatbubble.E33Log;
import net.minecraft.client.MinecraftClient;
//#if MC < 26000
import net.minecraft.client.option.KeyBinding;
//#else
//$$ import net.minecraft.client.KeyMapping;
//#endif

import javax.swing.SwingUtilities;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public final class NativeFileDialog {
    private static boolean open;

    private NativeFileDialog() {}

    public static void pickImage(java.util.function.Consumer<File> callback) {
        if (open) return;
        open = true;
        KeyBinding.unpressAll();
        MinecraftClient mc = MinecraftClient.getInstance();

        //#if MC >= 26000
        if (mc.mouseHandler != null) ((com.niuqu.chatbubble.mixin.MouseHandlerAccessor) mc.mouseHandler).e33chat$setActiveButton(false);
        //#else
        //$$ if (mc.mouse != null) ((com.niuqu.chatbubble.mixin.MouseHandlerAccessor) mc.mouse).e33chat$setActiveButton(0);
        //#endif

        Thread t = new Thread(() -> {
            AtomicReference<File> picked = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    FileDialog fd = new FileDialog((Frame) null, "Select emote image");
                    fd.setFilenameFilter((dir, name) -> {
                        String n = name.toLowerCase();
                        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
                    });
                    fd.setVisible(true);
                    File[] files = fd.getFiles();
                    if (files != null && files.length > 0) picked.set(files[0]);
                    fd.dispose();
                });
            } catch (Exception e) {
                E33Log.warn("[e33chat] File dialog failed", e);
            } finally {
                open = false;
            }
            File result = picked.get();
            mc.execute(() -> callback.accept(result));
        }, "e33chat-file-dialog");
        t.setDaemon(true);
        t.start();
    }
}
