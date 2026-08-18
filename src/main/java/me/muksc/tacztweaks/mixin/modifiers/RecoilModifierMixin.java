package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.resource.modifier.custom.RecoilModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Common cache hooks; client-only property diagrams live in RecoilModifierDiagramMixin. */
@Mixin(value = RecoilModifier.class, remap = false)
public abstract class RecoilModifierMixin {
    @ModifyExpressionValue(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/custom/RecoilModifier;getMaxInGunRecoilKeyFrame([Lcom/tacz/guns/resource/pojo/data/gun/GunRecoilKeyFrame;)F", ordinal = 0))
    private float tacztweaks$initCache$verticalRecoilModifier(float original) {
        return (float) Config.Modifiers.VerticalRecoil.INSTANCE.eval(original);
    }

    @ModifyExpressionValue(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/custom/RecoilModifier;getMaxInGunRecoilKeyFrame([Lcom/tacz/guns/resource/pojo/data/gun/GunRecoilKeyFrame;)F", ordinal = 1))
    private float tacztweaks$initCache$horizontalRecoilModifier(float original) {
        return (float) Config.Modifiers.HorizontalRecoil.INSTANCE.eval(original);
    }
}
