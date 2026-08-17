package me.muksc.tacztweaks.effect

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity


class FlagStatusEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color) {
    override fun applyEffectTick(livingEntity: LivingEntity, amplifier: Int): Boolean = false
}
