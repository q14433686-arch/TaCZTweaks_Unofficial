package me.muksc.tacztweaks.mixin.feature.balancing.aim_time;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AdsModifier.class, remap = false)
public abstract class AdsModifierMixin {
    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "com/tacz/guns/api/modifier/CacheValue.<init>(Ljava/lang/Object;)V"))
    private Object tacztweaks$initCache$aimTimeModifier(Object value) {
        if (!(value instanceof Float aimTime)) return value;
        return (float) Config.Balancing.AimTime.eval(aimTime);
    }

    /** Keep diagrams consistent without capturing javac locals. */
    @ModifyExpressionValue(
        method = "getPropertyDiagramsData",
        at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAimTime()F")
    )
    private float tacztweaks$getPropertyDiagramsData$aimTimeModifier(float original) {
        return (float) Config.Balancing.AimTime.eval(original);
    }
}
