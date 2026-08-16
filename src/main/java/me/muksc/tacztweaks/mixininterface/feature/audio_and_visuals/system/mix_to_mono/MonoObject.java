package me.muksc.tacztweaks.mixininterface.feature.audio_and_visuals.system.mix_to_mono;

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation;

public interface MonoObject {
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    static MonoObject of(ResourceLocation instance) {
        return /*? if <1.21 {*/ (MonoObject) instance /*?} else {*/ /*MonoObject.class.cast(instance) *//*?}*/;
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