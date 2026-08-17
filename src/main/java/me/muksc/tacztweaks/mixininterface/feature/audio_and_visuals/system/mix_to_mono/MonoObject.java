package me.muksc.tacztweaks.mixininterface.feature.audio_and_visuals.system.mix_to_mono;

import net.minecraft.resources.Identifier;

public interface MonoObject {
    static MonoObject of(Identifier instance) {
        return MonoObject.class.cast(instance);
    }

    /**
     * @param instance TaczSound
     */
    static MonoObject of(Object instance) {
        return (MonoObject) instance;
    }

    boolean tacztweaks$getMono();

    void tacztweaks$setMono(boolean mono);
}