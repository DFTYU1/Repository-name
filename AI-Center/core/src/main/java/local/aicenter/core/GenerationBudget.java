package local.aicenter.core;

/** Applies capability-wide generation limits without inspecting question ids or answer keys. */
public final class GenerationBudget {
    /*
     * Keep enough room for compact multi-part answers while staying below the
     * 180 second per-question watchdog on the verified ~5 token/s runtime.
     */
    private static final int LONG_FORM_MINIMUM = 640;
    private static final int HARD_MAXIMUM = 1024;
    private GenerationBudget() {}

    public static int forKind(String kind, int requested) {
        if (kind == null || requested < 1 || requested > HARD_MAXIMUM) throw new IllegalArgumentException("Invalid generation budget");
        if (kind.equals("manual")) return Math.max(requested, LONG_FORM_MINIMUM);
        return requested;
    }
}
