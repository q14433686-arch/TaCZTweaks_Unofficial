package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.system.mix_to_mono;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.client.sound.GunSoundInstance;
import me.muksc.tacztweaks.mixininterface.feature.audio_and_visuals.system.mix_to_mono.MonoObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunSoundInstance.class)
public abstract class GunSoundInstanceMixin {
    @Unique
    private boolean tacztweaks$mono = false;

    /** Both TaCZ constructors carry mono as their first boolean argument. */
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tacztweaks$init$mixToMono$storeMono(
        CallbackInfo ci,
        @Local(argsOnly = true, ordinal = 0) boolean mono
    ) {
        tacztweaks$mono = mono;
    }

    @ModifyExpressionValue(method = "resolve", at = @At(value = "NEW", target = "com/tacz/guns/client/sound/GunSoundInstance$TaczSound", remap = false))
    private @Coerce Object tacztweaks$resolve$mixToMono$setMono(@Coerce Object original) {
        MonoObject object = MonoObject.of(original);
        object.tacztweaks$setMono(tacztweaks$mono);
        return original;
    }
}
