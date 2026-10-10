package com.niuqu.chatbubble.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL30;
//#if MC >= 12000
import net.minecraft.client.gui.DrawContext;
//#else
//$$ import com.niuqu.chatbubble.DrawContext;
//#endif

public class BlurRenderer {

    private static boolean disconnecting = false;

    public static boolean isDisconnecting() { return disconnecting; }
    public static void setDisconnecting(boolean v) { disconnecting = v; }

    private static int fbo0 = -1, tex0 = -1;
    private static int fbo1 = -1, tex1 = -1;
    private static int fbo2 = -1, tex2 = -1;
    private static int fbo3 = -1, tex3 = -1;
    private static int fbo4 = -1, tex4 = -1;
    private static int cw, ch;

    private static boolean nextFull = true;
    private static boolean recreated = true;

    private static int[] make(int w, int h) {
        w = Math.max(1, w); h = Math.max(1, h);
        int fbo = GL30.glGenFramebuffers();
        int tex = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, tex);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);

        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, w, h, 0,
            GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_TEXTURE_2D, tex, 0);
        return new int[]{fbo, tex};
    }

    private static void ensure(int pw, int ph) {
        int mw = pw, mh = ph;
        if (mw == cw && mh == ch) return;
        destroy();
        recreated = true;
        cw = mw; ch = mh;
        int[] a = make(mw,     mh);     fbo0 = a[0]; tex0 = a[1];
        int[] b = make(mw / 2, mh / 2); fbo1 = b[0]; tex1 = b[1];
        int[] c = make(mw / 4, mh / 4); fbo2 = c[0]; tex2 = c[1];
        int[] d = make(mw / 8, mh / 8); fbo3 = d[0]; tex3 = d[1];
        int[] e = make(mw / 16, mh / 16); fbo4 = e[0]; tex4 = e[1];
    }

    private static void destroy() {
        if (fbo0 != -1) {
            GL30.glDeleteFramebuffers(fbo0); GL30.glDeleteTextures(tex0);
            GL30.glDeleteFramebuffers(fbo1); GL30.glDeleteTextures(tex1);
            GL30.glDeleteFramebuffers(fbo2); GL30.glDeleteTextures(tex2);
            GL30.glDeleteFramebuffers(fbo3); GL30.glDeleteTextures(tex3);
            GL30.glDeleteFramebuffers(fbo4); GL30.glDeleteTextures(tex4);
            fbo0 = -1; cw = ch = 0;
        }
    }

    private static void blit(int sfbo, int sx0, int sy0, int sx1, int sy1,
                              int dfbo, int dx0, int dy0, int dw, int dh) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sfbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dfbo);
        GL30.glBlitFramebuffer(sx0, sy0, sx1, sy1, dx0, dy0, dx0 + dw, dy0 + dh,
            GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
    }

    public static void cleanup() {
        destroy();
        nextFull = true;
        recreated = true;
    }

    public static void blurPanel(DrawContext g, int x, int y, int w, int h) {
        //#if MC >= 12111

        if (g != null) {
            //#if MC >= 26000

            g.blurBeforeThisStratum();
            //#else
            g.applyBlur();
            //#endif
            return;
        }
        //#endif
        var mc = MinecraftClient.getInstance();
        if (disconnecting || mc == null || mc.world == null || mc.player == null) return;

        int oldFb = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] vp = new int[4];
        GL30.glGetIntegerv(GL30.GL_VIEWPORT, vp);
        boolean scissor = GL30.glIsEnabled(GL30.GL_SCISSOR_TEST);
        try {
            blurRegion(mc, x, y, w, h);
        } catch (Throwable t) {
            com.niuqu.chatbubble.E33Log.debug("[e33chat] panel blur failed: {}", t.toString());
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFb);
            GL30.glViewport(vp[0], vp[1], vp[2], vp[3]);
            if (scissor) GL30.glEnable(GL30.GL_SCISSOR_TEST);
        }
    }

    private static void blurRegion(MinecraftClient mc, int x, int y, int w, int h) {
        //#if MC >= 12105

        int mainFb = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        //#else
        //$$ int mainFb = mc.getFramebuffer().fbo;
        //#endif
        //#if MC >= 26000

        int fbH = mc.getWindow().getHeight();
        //#else
        int fbH = mc.getFramebuffer().textureHeight;
        //#endif
        if (w <= 0 || h <= 0) return;

        double s = mc.getWindow().getScaleFactor();
        x = (int) Math.round(x * s);
        y = (int) Math.round(y * s);
        w = (int) Math.round(w * s);
        h = (int) Math.round(h * s);

        int y2 = Math.min(y + h, fbH);
        if (y2 <= y) return;
        h = y2 - y;
        ensure(w, h);
        int glY0 = fbH - (y + h);
        int glY1 = fbH - y;

        GL30.glDisable(GL30.GL_SCISSOR_TEST);

        boolean full = nextFull || recreated;
        nextFull = !full;
        if (full) {
            blit(mainFb, x, glY0, x + w, glY1, fbo0, 0, 0, w, h);

            blit(fbo0, 0, 0, w, h,    fbo1, 0, 0, w / 2, h / 2);
            blit(fbo1, 0, 0, w / 2, h / 2, fbo2, 0, 0, w / 4, h / 4);
            blit(fbo2, 0, 0, w / 4, h / 4, fbo3, 0, 0, w / 8, h / 8);
            blit(fbo3, 0, 0, w / 8, h / 8, fbo4, 0, 0, w / 16, h / 16);

            blit(fbo4, 0, 0, w / 16, h / 16, fbo3, 0, 0, w / 8, h / 8);
            blit(fbo3, 0, 0, w / 8, h / 8, fbo2, 0, 0, w / 4, h / 4);
            blit(fbo2, 0, 0, w / 4, h / 4, fbo1, 0, 0, w / 2, h / 2);
            blit(fbo1, 0, 0, w / 2, h / 2, fbo0, 0, 0, w, h);
            recreated = false;
        }

        blit(fbo0, 0, 0, w, h, mainFb, x, glY0, w, h);
    }
}
