package local.aicenter.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** One revocation boundary for agent work, file streams and future network/voice adapters. */
public final class StopController {
    private long generation = 0;
    private boolean enabled = true;
    private final Map<String, Runnable> hooks = new LinkedHashMap<>();

    public static final class Stopped extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public Stopped() { super("任务已停止"); }
    }
    public final class Token {
        private final long epoch;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private Token(long epoch) { this.epoch = epoch; }
        public void cancel() { cancelled.set(true); }
        public boolean valid() {
            synchronized (StopController.this) { return enabled && epoch == generation && !cancelled.get(); }
        }
        public void check() { if (!valid() || Thread.currentThread().isInterrupted()) throw new Stopped(); }
    }
    public synchronized Token begin() {
        if (!enabled) throw new Stopped();
        return new Token(generation);
    }
    public synchronized boolean enabled() { return enabled; }
    public synchronized void resume() { enabled = true; }
    public AutoCloseable onStop(Token token, Runnable hook) {
        String id = UUID.randomUUID().toString();
        synchronized (this) {
            token.check();
            hooks.put(id, hook);
        }
        return () -> { synchronized (StopController.this) { hooks.remove(id); } };
    }
    public void stop() {
        List<Runnable> callbacks;
        synchronized (this) {
            enabled = false;
            generation++;
            callbacks = new ArrayList<>(hooks.values());
            hooks.clear();
        }
        for (Runnable callback : callbacks) {
            try { callback.run(); } catch (RuntimeException ignored) { /* Continue revoking other resources. */ }
        }
    }
}
