package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.DamageModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the global damage modifier to the base damage stored in the
 * {@link com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair} cache.
 *
 * <p>{@code initCache} builds the cache in two places (verified against the 26.2 bytecode):
 * the distance-falloff loop (ordinal 0) and the plain-damage branch (ordinal 1). Both
 * {@code DistanceDamagePair} constructions must be modified, otherwise guns without
 * distance falloff would keep their unmodified damage.</p>
 */
@Mixin(value = DamageModifier.class, remap = false)
public abstract class DamageModifierMixin {
    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/ExtraDamage$DistanceDamagePair;<init>(FF)V", ordinal = 0), index = 1)
    private float tacztweaks$initCache$damageModifier$falloff(float damage) {
        return (float) Config.Modifiers.Damage.INSTANCE.eval(damage);
    }

    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/ExtraDamage$DistanceDamagePair;<init>(FF)V", ordinal = 1), index = 1)
    private float tacztweaks$initCache$damageModifier$plain(float damage) {
        return (float) Config.Modifiers.Damage.INSTANCE.eval(damage);
    }

    @Inject(
        method = "getPropertyDiagramsData",
        at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/ExtraDamage$DistanceDamagePair;getDamage()F", ordinal = 1)
    )
    private void tacztweaks$getPropertyDiagramsData$damageModifier(
        ItemStack gunItem,
        GunData gunData,
        AttachmentCacheProperty cacheProperty,
        CallbackInfoReturnable<?> cir,
        @Local(ordinal = 1) LocalFloatRef finalBase
    ) {
        finalBase.set((float) Config.Modifiers.Damage.INSTANCE.eval(finalBase.get()));
    }
}
