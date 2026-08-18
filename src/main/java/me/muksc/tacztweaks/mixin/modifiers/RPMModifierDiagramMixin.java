package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.resource.modifier.custom.RpmModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Client-only baseline adjustment for the refit-screen RPM diagram. */
@Mixin(value = RpmModifier.class, remap = false)
public abstract class RPMModifierDiagramMixin {
    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getRoundsPerMinute(Lcom/tacz/guns/api/item/gun/FireMode;)I"))
    private int tacztweaks$getPropertyDiagramsData$rpmModifier(int original) {
        return (int) Config.Modifiers.RPM.INSTANCE.eval(original);
    }
}
