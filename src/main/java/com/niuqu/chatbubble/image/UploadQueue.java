package com.niuqu.chatbubble.image;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.E33Log;
import java.io.File;
import java.util.ArrayDeque;
import net.minecraft.client.MinecraftClient;

// 上传任务队列
public final class UploadQueue {
    public record UploadJob(File file, byte[] bytes, String fileName,
                            boolean emote, String pendingText) {}

    public interface Callbacks {
        // 上传开始回调
        void onBusyStart();
        // 上传完成回调
        void onIdle();
        // 上传失败回调
        void onFailure();
        // 图片超限回调
        void onRejected(AnimatedImageLoader.OverBudget reason);
        // 表情发送回调
        void onEmoteSent(String url);
        // 发送文本回调
        void onSendText(String text);
        // 填入图片码回调
        void onInputImage(String code);
        // 恢复输入框回调
        void onRestoreInput(String text);
    }

    private static final int MAX_JOBS = 8;
    private final ArrayDeque<UploadJob> jobs = new ArrayDeque<>();
    private boolean running;
    private final Callbacks cb;

    // 绑定回调构造
    public UploadQueue(Callbacks cb) {
        this.cb = cb;
    }

    // 加入上传任务
    public boolean enqueue(UploadJob job) {
        if (jobs.size() >= MAX_JOBS) return false;
        jobs.addLast(job);
        drain();
        return true;
    }

    // 取待处理数量
    public int pending() { return jobs.size(); }

    // 启动下一个任务
    private void drain() {
        if (running) return;
        UploadJob job = jobs.pollFirst();
        if (job == null) return;
        running = true;
        cb.onBusyStart();
        ImageLoader.executor().execute(() -> {
            try {
                E33Log.info(
                    "[e33chat] upload start | file={} | emote={} | serverEnabled={}",
                    job.file() != null ? job.file().getName() : job.fileName(), job.emote(),
                    MediaClient.serverEnabled());
                LocalImageSource.PreparedImage prep;
                if (job.file() != null) {
                    LocalImageSource.Prep rep = LocalImageSource.prepare(job.file());
                    if (rep instanceof LocalImageSource.Prep.Rejected rejected) {
                        E33Log.info("[e33chat] upload rejected (animated over budget: {}) | file={}",
                            rejected.reason(), job.file().getName());
                        AnimatedImageLoader.OverBudget reason = rejected.reason();
                        MinecraftClient.getInstance().execute(() -> {
                            running = false;
                            cb.onRejected(reason);
                            if (job.pendingText() != null) cb.onRestoreInput(job.pendingText());
                            drain();
                        });
                        return;
                    }
                    prep = ((LocalImageSource.Prep.Ok) rep).image();
                } else {
                    prep = new LocalImageSource.PreparedImage(job.bytes(), job.fileName());
                }
                if (prep == null) {
                    MinecraftClient.getInstance().execute(() -> {
                        running = false;
                        cb.onFailure();
                        if (job.pendingText() != null) cb.onRestoreInput(job.pendingText());
                        drain();
                    });
                    return;
                }
                finish(job, prep);
            } catch (Throwable t) {
                E33Log.error("[e33chat] upload worker crashed", t);
                MinecraftClient.getInstance().execute(() -> {
                    running = false;
                    cb.onFailure();
                    if (job.pendingText() != null) cb.onRestoreInput(job.pendingText());
                    drain();
                });
            }
        });
    }

    // 上传完成后分发
    private void finish(UploadJob job, LocalImageSource.PreparedImage prep) {
        String serverUrl = MediaClient.serverEnabled()
            ? MediaClient.upload(prep.bytes(), prep.contentType())
            : null;
        var cfg = ChatBubbleClientSetup.config();
        final String url = serverUrl != null
            ? serverUrl
            : ImageUploader.upload(prep.bytes(), prep.fileName(),
                cfg.uploadUrl(), cfg.uploadField(), cfg.uploadExtra(), cfg.uploadResponse());
        E33Log.info("[e33chat] upload {} -> {}", prep.fileName(), url == null ? "FAILED" : url);
        MinecraftClient.getInstance().execute(() -> {
            running = false;
            if (url == null) {
                cb.onFailure();
                if (job.pendingText() != null) cb.onRestoreInput(job.pendingText());
                drain();
                return;
            }
            cb.onIdle();
            String nameAttr = "name=" + prep.fileName();
            if (job.emote()) {
                cb.onEmoteSent("[[E33Emote,url=" + url + "," + nameAttr + "]]");
            } else if (job.pendingText() != null) {
                String finalText = job.pendingText().replaceFirst(
                    "\\[\\[CICode,url=file://[^]]*]]", "[[CICode,url=" + url + "," + nameAttr + "]]");
                cb.onSendText(finalText);
            } else {
                String code = "[[CICode,url=" + url + "," + nameAttr + "]]";
                cb.onInputImage(code);
            }
            drain();
        });
    }
}
