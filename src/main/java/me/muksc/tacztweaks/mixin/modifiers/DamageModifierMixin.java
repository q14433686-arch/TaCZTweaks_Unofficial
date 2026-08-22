package me.muksc.tacztweaks.mixin.modifiers;

import com.tacz.guns.resource.modifier.custom.DamageModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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
}
