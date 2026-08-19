package me.muksc.tacztweaks.mixin.compat.soundphysics;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.sonicether.soundphysics.SoundPhysics;
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional hooks for Sound Physics Remastered 1.5.1 on 1.21.11. */
@Pseudo
@Mixin(value = SoundPhysics.class, remap = false)
public abstract class SoundPhysicsMixin {
    @Unique
    private static final String EVALUATE = "evaluateEnvironment";

    @Inject(method = EVALUATE, at = @At("HEAD"))
    private static void tacztweaks$evaluateEnvironment$begin(
        int sourceId,
        double x,
        double y,
        double z,
        SoundSource category,
        Identifier sound,
        boolean auxOnly,
        CallbackInfoReturnable<Vec3> cir
    ) {
        SoundPhysicsCompat.INSTANCE.begin(x, y, z, sound);
    }

    @ModifyExpressionValue(
        method = EVALUATE,
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicether/soundphysics/SoundPhysics;calculateOcclusion(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/resources/Identifier;)D",
            remap = true
        )
    )
    private static double tacztweaks$evaluateEnvironment$captureOcclusion(double original) {
        SoundPhysicsCompat.INSTANCE.captureOcclusion(original);
        return original;
    }

    @ModifyExpressionValue(
        method = EVALUATE,
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicether/soundphysics/ReflectedAudio;getSharedAirspaces()I",
            ordinal = 0,
            remap = true
        )
    )
    private static int tacztweaks$evaluateEnvironment$captureAirspace(int original) {
        SoundPhysicsCompat.INSTANCE.captureAirspaceCount(original);
        return original;
    }

    @Inject(
        method = EVALUATE,
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicether/soundphysics/SoundPhysics;setEnvironment(IFFFFFFFFFF)V"
        )
    )
    private static void tacztweaks$evaluateEnvironment$complete(
        int sourceId,
        double x,
        double y,
        double z,
        SoundSource category,
        Identifier sound,
        boolean auxOnly,
        CallbackInfoReturnable<Vec3> cir,
        @Local float[] bounceReflectivityRatio
    ) {
        SoundPhysicsCompat.INSTANCE.complete(bounceReflectivityRatio);
    }

    @Inject(method = EVALUATE, at = @At("RETURN"))
    private static void tacztweaks$evaluateEnvironment$clear(
        int sourceId,
        double x,
        double y,
        double z,
        SoundSource category,
        Identifier sound,
        boolean auxOnly,
        CallbackInfoReturnable<Vec3> cir
    ) {
        SoundPhysicsCompat.INSTANCE.clear();
    }
}
