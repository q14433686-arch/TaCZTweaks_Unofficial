package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.ArmorIgnoreModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArmorIgnoreModifier.class, remap = false)
public abstract class ArmorIgnoreModifierMixin {
    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/modifier/CacheValue;<init>(Ljava/lang/Object;)V"), index = 0)
    private Object tacztweaks$initCache$armorIgnoreModifier(Object value) {
        if (!(value instanceof Float armorIgnore)) return value;
        return (float) Config.Modifiers.ArmorIgnore.INSTANCE.eval(armorIgnore);
    }

    @Inject(
        method = "getPropertyDiagramsData",
        at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;")
    )
    private void tacztweaks$getPropertyDiagramsData$armorIgnoreModifier(
        ItemStack gunItem,
        GunData gunData,
        AttachmentCacheProperty cacheProperty,
        CallbackInfoReturnable<?> cir,
        @Local(ordinal = 0) LocalFloatRef finalBase
    ) {
        finalBase.set((float) Config.Modifiers.ArmorIgnore.INSTANCE.eval(finalBase.get()));
    }
}
