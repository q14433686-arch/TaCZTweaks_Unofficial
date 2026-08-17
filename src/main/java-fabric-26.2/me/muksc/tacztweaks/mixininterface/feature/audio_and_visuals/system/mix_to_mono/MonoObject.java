package me.muksc.tacztweaks.mixininterface.feature.audio_and_visuals.system.mix_to_mono;

import net.minecraft.resources.Identifier;

public interface MonoObject {
    static MonoObject of(Identifier instance) {
        // 26.2+: the `Identifier` class has both an interface injection (this mixin interface)
        // and Object superclass, so Class.cast() is needed for unambiguous resolution.
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
