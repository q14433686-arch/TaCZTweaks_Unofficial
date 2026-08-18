package me.muksc.tacztweaks.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

/**
 * A passive status effect that stops the affected player's guns from consuming ammo.
 * Actual logic lives in [me.muksc.tacztweaks.mixin.tweaks.LivingEntityAmmoCheckMixin].
 */
class EndlessAmmoStatusEffect : MobEffect(MobEffectCategory.BENEFICIAL, 0x3B8B3B)
