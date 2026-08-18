package me.muksc.tacztweaks.mixininterface.tweaks;

/**
 * Implemented by {@code com.tacz.guns.client.sound.GunSoundInstance$TaczSound}
 * so the {@code mono} flag captured from the {@code GunSoundInstance}
 * constructor can be carried onto the redirect sound and, from there, onto the
 * decoded sound buffer.
 */
public interface MonoTaczSound {
    boolean tacztweaks$getMono();

    void tacztweaks$setMono(boolean mono);
}
