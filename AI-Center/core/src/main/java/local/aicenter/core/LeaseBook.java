package local.aicenter.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory only: a process restart intentionally grants no external file access. */
public final class LeaseBook {
    public interface Clock { long elapsedMillis(); }
    public static final long GRACE_MS = 5 * 60 * 1000L;
    private static final class Lease {
        final String taskId, resource;
        final StopController.Token token;
        long deadline = Long.MAX_VALUE;
        Lease(String taskId, String resource, StopController.Token token) {
            this.taskId = taskId; this.resource = resource; this.token = token;
        }
    }
    private final Clock clock;
    private final Map<String, Lease> leases = new HashMap<>();
    public LeaseBook(Clock clock) { this.clock = clock; }
    public synchronized String grant(String taskId, String resource, StopController.Token token) {
        token.check();
        if (taskId == null || taskId.isEmpty() || resource == null || !resource.startsWith("content://"))
            throw new IllegalArgumentException("必须授权明确的文件");
        String id = UUID.randomUUID().toString();
        leases.put(id, new Lease(taskId, resource, token));
        return id;
    }
    public synchronized void check(String id, String resource) {
        Lease lease = leases.get(id);
        if (lease == null || !lease.resource.equals(resource) || !lease.token.valid() || clock.elapsedMillis() >= lease.deadline) {
            if (lease != null && (!lease.token.valid() || clock.elapsedMillis() >= lease.deadline)) leases.remove(id);
            throw new SecurityException("文件临时授权已失效");
        }
        lease.token.check();
    }
    public synchronized void complete(String taskId) {
        long deadline = Math.addExact(clock.elapsedMillis(), GRACE_MS);
        for (Lease lease : leases.values())
            if (lease.taskId.equals(taskId) && lease.deadline == Long.MAX_VALUE) lease.deadline = deadline;
    }
    public synchronized void revoke(String id) { leases.remove(id); }
    public synchronized void revokeAll() { leases.clear(); }
    public synchronized int count() {
        leases.entrySet().removeIf(e -> !e.getValue().token.valid() || clock.elapsedMillis() >= e.getValue().deadline);
        return leases.size();
    }
}
