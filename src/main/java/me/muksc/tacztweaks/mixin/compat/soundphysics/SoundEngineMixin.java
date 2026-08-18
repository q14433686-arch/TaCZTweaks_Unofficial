package me.muksc.tacztweaks.mixin.compat.soundphysics;

import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat;
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsTriggerSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 {@code SoundEngine#play} lambda names are not stable. Wrap the whole
 * {@code play(SoundInstance)} call instead: SPR evaluates the environment
 * synchronously inside it, so HEAD/RETURN is enough to capture airspace.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void tacztweaks$play$begin(SoundInstance sound, CallbackInfo ci) {
        if (sound instanceof SoundPhysicsTriggerSoundInstance trigger) {
            SoundPhysicsCompat.INSTANCE.setProcessingSound(new SoundPhysicsCompat.ProcessingSound(trigger));
        }
    }

    @Inject(method = "play", at = @At("RETURN"))
    private void tacztweaks$play$end(SoundInstance sound, CallbackInfo ci) {
        if (!(sound instanceof SoundPhysicsTriggerSoundInstance)) return;
        SoundPhysicsCompat.INSTANCE.onSoundEvaluationComplete();
        SoundPhysicsCompat.INSTANCE.setProcessingSound(null);
    }
}
