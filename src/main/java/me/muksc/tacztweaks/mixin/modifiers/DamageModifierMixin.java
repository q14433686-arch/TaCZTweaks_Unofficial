package me.muksc.tacztweaks.mixin.modifiers;

import com.tacz.guns.resource.modifier.custom.DamageModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Applies the global damage modifier to both common cache construction paths. Client-only
 * property-diagram handling lives in DamageModifierDiagramMixin.
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
