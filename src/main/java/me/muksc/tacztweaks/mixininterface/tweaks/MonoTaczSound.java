package me.muksc.tacztweaks.mixininterface.tweaks;

/**
 * Extra state on {@code GunSoundInstance$TaczSound} so we can remember that TaCZ asked
 * for a mono buffer without writing a field onto the 26.2 {@code Identifier} record.
 */
public interface MonoTaczSound {
    boolean tacztweaks$getMono();

    void tacztweaks$setMono(boolean mono);
}
