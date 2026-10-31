package com.niuqu.chatbubble.image;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

// 图片加载状态条目
public final class ImageEntry {
    public enum State { LOADING, LOADED, FAILED }

    private final String url;
    private volatile State state = State.LOADING;
    private volatile Identifier textureId;
    private volatile int width;
    private volatile int height;
    private volatile String failure;
    private volatile long failedAtMillis;

    // 构造条目
    ImageEntry(String url) {
        this.url = url;
    }

    // 取图片地址
    public String url() { return url; }
    // 取加载状态
    public State state() { return state; }
    // 取纹理标识
    public Identifier textureId() { return textureId; }
    // 取图片宽度
    public int width() { return width; }
    // 取图片高度
    public int height() { return height; }
    // 取失败原因
    public String failure() { return failure; }
    // 取失败时间
    public long failedAtMillis() { return failedAtMillis; }

    // 标记加载完成
    synchronized void markLoaded(Identifier id, NativeImage img) {
        this.textureId = id;
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.state = State.LOADED;
        ImageLoader.VERSION.incrementAndGet();
    }

    // 标记加载失败
    synchronized void markFailed(String reason) {
        this.failure = reason;
        this.state = State.FAILED;
        this.failedAtMillis = System.currentTimeMillis();
        ImageLoader.VERSION.incrementAndGet();
    }
}
