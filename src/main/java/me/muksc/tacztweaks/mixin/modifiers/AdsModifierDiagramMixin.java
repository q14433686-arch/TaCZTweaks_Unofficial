package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AdsModifier.class, remap = false)
public abstract class AdsModifierDiagramMixin {
    @Inject(method = "getPropertyDiagramsData", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;"))
    private void tacztweaks$getPropertyDiagramsData$aimTimeModifier(
        ItemStack gunItem,
        GunData gunData,
        AttachmentCacheProperty cacheProperty,
        CallbackInfoReturnable<?> cir,
        @Local(ordinal = 0) LocalFloatRef aimTime
    ) {
        aimTime.set((float) Config.Modifiers.AimTime.INSTANCE.eval(aimTime.get()));
    }
}
