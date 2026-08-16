package me.muksc.tacztweaks.feature.datapack;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Per-thread projectile metadata for the synchronous TaCZ spawnProjectiles transaction. */
public final class BulletIndexContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private BulletIndexContext() { }

    public static Scope open(int burstIndex, ItemStack gunStack) {
        State previous = CURRENT.get();
        CURRENT.set(new State(burstIndex, gunStack));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public static @Nullable ProjectileData next() {
        State state = CURRENT.get();
        if (state == null) return null;
        return new ProjectileData(state.burstIndex, state.pelletIndex++, state.gunStack);
    }

    public record ProjectileData(int burstIndex, int pelletIndex, ItemStack gunStack) { }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    private static final class State {
        private final int burstIndex;
        private final ItemStack gunStack;
        private int pelletIndex;

        private State(int burstIndex, ItemStack gunStack) {
            this.burstIndex = burstIndex;
            this.gunStack = gunStack;
        }
    }
}
