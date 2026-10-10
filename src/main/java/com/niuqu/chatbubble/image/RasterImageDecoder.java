package com.niuqu.chatbubble.image;

import com.niuqu.chatbubble.E33Log;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.texture.NativeImage;

public final class RasterImageDecoder {
    public static final int MAX_DIMENSION = 4096;

    public record DecodedImage(NativeImage image, int width, int height) {}

    private RasterImageDecoder() {}

    public static DecodedImage decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
                if (img == null) return null;
                return new DecodedImage(img, img.getWidth(), img.getHeight());
            }

            try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (!readers.hasNext()) return null;
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in, true, true);
                    int w = reader.getWidth(0);
                    int h = reader.getHeight(0);
                    if (w <= 0 || h <= 0 || w > MAX_DIMENSION || h > MAX_DIMENSION) return null;
                    BufferedImage bi;
                    try {
                        bi = reader.read(0);
                    } catch (Throwable t) {
                        return null;
                    }
                    if (bi == null) return null;
                    return new DecodedImage(fromBufferedImage(bi), bi.getWidth(), bi.getHeight());
                } finally {
                    reader.dispose();
                }
            }
        } catch (Throwable t) {
            E33Log.debug("[e33chat] image decode failed: {}", t.toString());
            return null;
        }
    }

    public static NativeImage fromBufferedImage(BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, w, h, false);

        int[] argb = bi.getRGB(0, 0, w, h, null, 0, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = argb[y * w + x];
                int abgr = (c & 0xFF00FF00) | ((c & 0x00FF0000) >> 16) | ((c & 0x000000FF) << 16);
                //#if MC >= 12102
                out.setColorArgb(x, y, abgr);
                //#else
                //#if MC >= 11800
                //$$ out.setColor(x, y, abgr);
                //#else
                //$$ out.setPixelColor(x, y, abgr);
                //#endif
                //#endif
            }
        }
        return out;
    }
}
