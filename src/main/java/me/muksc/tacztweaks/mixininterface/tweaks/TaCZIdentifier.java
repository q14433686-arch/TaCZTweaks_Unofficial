package me.muksc.tacztweaks.mixininterface.tweaks;

/**
 * Marks an {@link net.minecraft.resources.Identifier} (the 26.2 rename of
 * {@code ResourceLocation}) as a sound that should be down-mixed to mono when
 * its buffer is decoded.
 *
 * <p>Upstream (Forge 1.20.1) named this {@code TaCZResourceLocation}; it is
 * renamed here to follow the 26.2 {@code ResourceLocation -> Identifier} rename.</p>
 */
public interface TaCZIdentifier {
    boolean tacztweaks$getMonoAudio();

    void tacztweaks$setMonoAudio(boolean monoAudio);
}
