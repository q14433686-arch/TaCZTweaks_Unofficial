package me.muksc.tacztweaks.mixin.compat.soundphysics;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * String-targeted so this compiles without the SPR jar. Only applied when
 * {@code sound_physics_remastered} is loaded (see {@code ModMixinPlugin}).
 *
 * Avoids MixinExtras {@code @Expression} (runtime-fragile). Captures:
 * <ul>
 *   <li>occlusion via {@code calculateOcclusion} (1.5.x API, which 26.2 1.5.1 still has);</li>
 *   <li>shared airspaces via {@code ReflectedAudio#getSharedAirspaces};</li>
 *   <li>bounce reflectivity via {@code bounceReflectivityRatio} field gets, if present.</li>
 * </ul>
 */
@Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
public abstract class SoundPhysicsMixin {
    @ModifyExpressionValue(
        method = "evaluateEnvironment",
        at = @At(value = "INVOKE", target = "Lcom/sonicether/soundphysics/SoundPhysics;calculateOcclusion(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/resources/Identifier;)D")
    )
    private static double tacztweaks$evaluateEnvironment$setOcclusion(double original) {
        SoundPhysicsCompat.ProcessingSound sound = SoundPhysicsCompat.INSTANCE.getProcessingSound();
        if (sound != null) sound.setOcclusionAccumulation(original);
        return original;
    }

    @ModifyExpressionValue(
        method = "evaluateEnvironment",
        at = @At(value = "INVOKE", target = "Lcom/sonicether/soundphysics/ReflectedAudio;getSharedAirspaces()I")
    )
    private static int tacztweaks$evaluateEnvironment$setAirspace(int original) {
        SoundPhysicsCompat.ProcessingSound sound = SoundPhysicsCompat.INSTANCE.getProcessingSound();
        if (sound != null) sound.setAirspace(original * 64.0F);
        return original;
    }

    @Inject(method = "evaluateEnvironment", at = @At("RETURN"), require = 0)
    private static void tacztweaks$evaluateEnvironment$onReturn(
        int sourceID, double posX, double posY, double posZ,
        SoundSource category, Identifier sound, boolean auxOnly,
        CallbackInfoReturnable<Vec3> cir
    ) {
        // Capture is done; SoundEngineMixin RETURN fires the actual playback.
    }
}
