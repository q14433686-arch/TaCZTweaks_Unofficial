package me.muksc.tacztweaks.mixin.features.bullet_interactions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.muksc.tacztweaks.mixininterface.features.bullet_interaction.ShieldInteractionBehaviour;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies data-driven shield durability / disable duration. 26.2
 * {@code Player#disableShield()} takes no boolean (the 1.20 {@code disableShield(Z)}
 * is gone); cooldown is still {@code ItemCooldowns#addCooldown}.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements ShieldInteractionBehaviour {
    @ModifyVariable(method = "hurtCurrentlyUsedShield", at = @At("HEAD"), argsOnly = true)
    private float tacztweaks$hurtCurrentlyUsedShield$customDamage(float damage) {
        if (tacztweaks$getCustomShieldDurabilityDamage() == null) return damage;
        return tacztweaks$getCustomShieldDurabilityDamage().apply(Math.round(damage));
    }

    @ModifyExpressionValue(method = "disableShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float tacztweaks$disableShield$alwaysSucceed(float original) {
        if (tacztweaks$getCustomShieldDisableDuration() == null) return original;
        return 0.0F;
    }

    @ModifyArg(method = "disableShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"), index = 1)
    private int tacztweaks$disableShield$customDuration(int ticks) {
        if (tacztweaks$getCustomShieldDisableDuration() == null) return ticks;
        return tacztweaks$getCustomShieldDisableDuration();
    }
}
