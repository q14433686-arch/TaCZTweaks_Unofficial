package me.muksc.tacztweaks.mixininterface.tweaks;

/**
 * Extra state on {@code GunSoundInstance$TaczSound} so we can remember that TaCZ asked
 * for a mono buffer without attaching per-playback mutable state to a shared {@code Identifier}.
 */
public interface MonoTaczSound {
    boolean tacztweaks$getMono();

    void tacztweaks$setMono(boolean mono);
}
