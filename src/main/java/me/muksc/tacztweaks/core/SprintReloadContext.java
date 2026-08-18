package me.muksc.tacztweaks.core;

/** Thread-local scope used while vanilla applies the player's sprint input. */
public final class SprintReloadContext {
    private static final ThreadLocal<Boolean> APPLYING_SPRINT = ThreadLocal.withInitial(() -> false);

    private SprintReloadContext() {
    }

    public static boolean isApplyingSprint() {
        return APPLYING_SPRINT.get();
    }

    public static void run(Runnable action) {
        boolean previous = APPLYING_SPRINT.get();
        APPLYING_SPRINT.set(true);
        try {
            action.run();
        } finally {
            APPLYING_SPRINT.set(previous);
        }
    }
}
