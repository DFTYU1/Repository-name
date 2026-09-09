package local.aicenter.core;

public final class StorageBudget {
    public static final long TARGET_BYTES = 30_000_000_000L;
    public static final long FREE_RESERVE_BYTES = 256_000_000L;
    private StorageBudget() {}
    public static void check(long currentAppBytes, long incomingBytes, long freeDiskBytes) {
        if (currentAppBytes < 0 || incomingBytes < 0 || freeDiskBytes < 0) throw new IllegalArgumentException("空间数据无效");
        if (currentAppBytes > TARGET_BYTES || incomingBytes > TARGET_BYTES-currentAppBytes)
            throw new IllegalStateException("将超过 30GB 应用空间目标，请先管理存储");
        if (freeDiskBytes < FREE_RESERVE_BYTES || incomingBytes > freeDiskBytes-FREE_RESERVE_BYTES)
            throw new IllegalStateException("设备可用空间不足");
    }
}
