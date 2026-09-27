package local.aicenter.core;

/** Applies capability-wide generation limits without inspecting question ids or answer keys. */
public final class GenerationBudget {
    private static final int LONG_FORM_MINIMUM = 512;
    private static final int HARD_MAXIMUM = 1024;
    private GenerationBudget() {}

    public static int forKind(String kind, int requested) {
        if (kind == null || requested < 1 || requested > HARD_MAXIMUM) throw new IllegalArgumentException("Invalid generation budget");
        if (kind.equals("manual")) return Math.max(requested, LONG_FORM_MINIMUM);
        return requested;
    }
}
