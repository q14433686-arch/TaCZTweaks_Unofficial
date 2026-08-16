package me.muksc.tacztweaks.mixin.feature.gameplay.behaviour.tilt_rework;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// priority = 1500 because it has to be applied before the tilt gun key mixin
// `tiltGunKey(tiltRework(shouldSlide()))`
@Mixin(value = GunAnimationStateContext.class, priority = 1500, remap = false)
public abstract class GunAnimationStateContextMixin {
    @ModifyReturnValue(method = "shouldSlide", at = @At("RETURN"))
    private boolean tacztweaks$shouldSlide$tiltRework(boolean original) {
        return !Config.Gameplay.Behaviour.tiltRework() && original;
    }
}
