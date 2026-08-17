package me.muksc.tacztweaks.registry

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.effect.EndlessAmmoStatusEffect
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.world.effect.MobEffect

object ModStatusEffects {
    @JvmField
    val ENDLESS_AMMO: Holder<MobEffect> = Registry.registerForHolder(
        BuiltInRegistries.MOB_EFFECT,
        TaCZTweaks.id("endless_ammo"),
        EndlessAmmoStatusEffect()
    )
}
