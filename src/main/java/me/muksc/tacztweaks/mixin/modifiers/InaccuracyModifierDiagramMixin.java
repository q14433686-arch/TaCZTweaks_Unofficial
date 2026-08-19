package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InaccuracyModifier.class, remap = false)
public abstract class InaccuracyModifierDiagramMixin {
    @Inject(method = "buildNormal", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;"))
    private void tacztweaks$buildNormal$inaccuracyModifier(
        GunData gunData,
        AttachmentCacheProperty cacheProperty,
        GunFireModeAdjustData fireModeAdjustData,
        InaccuracyType type,
        String titleKey,
        double referenceValue,
        CallbackInfoReturnable<?> cir,
        @Local(ordinal = 0) LocalFloatRef inaccuracy
    ) {
        inaccuracy.set((float) Config.Modifiers.Inaccuracy.INSTANCE.eval(inaccuracy.get()));
    }

    @Inject(method = "buildAim", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;"))
    private void tacztweaks$buildAim$inaccuracyModifier(
        GunData gunData,
        AttachmentCacheProperty cacheProperty,
        GunFireModeAdjustData fireModeAdjustData,
        CallbackInfoReturnable<?> cir,
        @Local(ordinal = 0) LocalFloatRef aimInaccuracy
    ) {
        float accuracy = 1.0F - aimInaccuracy.get();
        accuracy = (float) Config.Modifiers.Inaccuracy.INSTANCE.eval(accuracy);
        accuracy = (float) Config.Modifiers.AimInaccuracy.INSTANCE.eval(accuracy);
        aimInaccuracy.set(1.0F - accuracy);
    }
}
