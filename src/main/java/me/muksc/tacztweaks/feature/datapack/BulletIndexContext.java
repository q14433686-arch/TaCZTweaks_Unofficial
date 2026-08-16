package me.muksc.tacztweaks.feature.datapack;

import org.jetbrains.annotations.Nullable;

/** Per-thread projectile indices for the synchronous TaCZ spawnProjectiles transaction. */
public final class BulletIndexContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private BulletIndexContext() { }

    public static Scope open(int burstIndex) {
        State previous = CURRENT.get();
        CURRENT.set(new State(burstIndex));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public static @Nullable Indices next() {
        State state = CURRENT.get();
        if (state == null) return null;
        return new Indices(state.burstIndex, state.pelletIndex++);
    }

    public record Indices(int burstIndex, int pelletIndex) { }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    private static final class State {
        private final int burstIndex;
        private int pelletIndex;

        private State(int burstIndex) {
            this.burstIndex = burstIndex;
        }
    }
}
