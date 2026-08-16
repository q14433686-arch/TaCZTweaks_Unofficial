package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.system.mix_to_mono;

import me.muksc.tacztweaks.mixininterface.feature.audio_and_visuals.system.mix_to_mono.MonoObject;
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
@Mixin(ResourceLocation.class)
public abstract class ResourceLocationMixin implements MonoObject {
    @Unique
    private boolean tacztweaks$mono = false;

    @Override
    public boolean tacztweaks$getMono() {
        return tacztweaks$mono;
    }

    @Override
    public void tacztweaks$setMono(boolean mono) {
        tacztweaks$mono = mono;
    }
}