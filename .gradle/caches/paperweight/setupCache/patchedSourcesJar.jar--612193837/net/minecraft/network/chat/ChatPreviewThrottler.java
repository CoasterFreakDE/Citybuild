package net.minecraft.network.chat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

public class ChatPreviewThrottler {
    private final AtomicReference<ChatPreviewThrottler.Request> scheduledRequest = new AtomicReference<>();
    @Nullable
    private CompletableFuture<?> runningRequest;

    public void tick() {
        if (this.runningRequest != null && this.runningRequest.isDone()) {
            this.runningRequest = null;
        }

        if (this.runningRequest == null) {
            this.tickIdle();
        }

    }

    private void tickIdle() {
        ChatPreviewThrottler.Request request = this.scheduledRequest.getAndSet((ChatPreviewThrottler.Request)null);
        if (request != null) {
            this.runningRequest = request.run();
        }

    }

    public void schedule(ChatPreviewThrottler.Request task) {
        this.scheduledRequest.set(task);
    }

    @FunctionalInterface
    public interface Request {
        CompletableFuture<?> run();
    }
}
