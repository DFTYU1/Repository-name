package local.aicenter.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Planning only. The Android adapter may delete only entries in its own cache directory. */
public final class CachePolicy {
    public static final long DEFAULT_LIMIT = 8_000_000_000L;
    public static final long EXPIRE_MS = 15L * 24 * 60 * 60 * 1000;
    public enum Kind { CACHE, FAVORITE, DOWNLOAD, KNOWLEDGE, NOTE, WORK_FILE, MISTAKE, USER_UPLOAD }
    public static final class Entry {
        public final String id;
        public final long size, lastUsed;
        public final Kind kind;
        public Entry(String id, long size, long lastUsed, Kind kind) {
            if (size < 0 || lastUsed < 0 || kind == null) throw new IllegalArgumentException("缓存元数据无效");
            this.id = id; this.size = size; this.lastUsed = lastUsed; this.kind = kind;
        }
    }
    public List<String> evict(List<Entry> entries, long limit, long now) {
        if (limit < 0 || now < 0) throw new IllegalArgumentException("空间或时间无效");
        List<Entry> cache = new ArrayList<>();
        long total = 0;
        for (Entry e : entries) if (e.kind == Kind.CACHE) { cache.add(e); total = Math.addExact(total, e.size); }
        cache.sort(Comparator.comparingLong((Entry e) -> e.lastUsed).thenComparing(e -> e.id));
        List<String> removed = new ArrayList<>();
        for (Entry e : cache) if ((now >= e.lastUsed && now-e.lastUsed >= EXPIRE_MS) || total > limit) {
            removed.add(e.id); total -= e.size;
        }
        return removed;
    }
}
