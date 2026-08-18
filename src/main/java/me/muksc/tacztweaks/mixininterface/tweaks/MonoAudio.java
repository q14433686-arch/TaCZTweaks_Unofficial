package me.muksc.tacztweaks.mixininterface.tweaks;

import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sidecar for {@code betterMonoConversion}. 26.2 {@link Identifier} is a record,
 * so we cannot {@code @Unique} a field onto it. Gun sound paths that should be
 * downmixed are registered here by value (Identifiers are tiny and intern-ish).
 */
public final class MonoAudio {
    private static final Set<Identifier> MONO = ConcurrentHashMap.newKeySet();

    private MonoAudio() {}

    public static void mark(Identifier id) {
        if (id != null) MONO.add(id);
    }

    public static boolean isMarked(Identifier id) {
        return id != null && MONO.contains(id);
    }
}
