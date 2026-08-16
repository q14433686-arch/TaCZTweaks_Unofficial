package me.muksc.tacztweaks.mixin.feature.keyactions.tiltgun;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public abstract class GunAnimationStateContextMixin {
    @Shadow private GunData gunData;

    @ModifyReturnValue(method = "shouldSlide", at = @At("RETURN"))
    private boolean tacztweaks$shouldSlide$tiltGunKey(boolean original) {
        return original || (gunData.canSlide() && TiltGunKey.KEY.isDown());
    }
}
