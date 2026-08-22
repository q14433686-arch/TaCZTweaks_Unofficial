package me.muksc.tacztweaks.registry

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.effect.EndlessAmmoStatusEffect
import net.minecraft.core.registries.Registries
import net.minecraft.world.effect.MobEffect
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/** NeoForge 注册面：静态 RegisterEvent 写入替换为 DeferredRegister。 */
object ModStatusEffects {
    @JvmField
    val EFFECTS: DeferredRegister<MobEffect> = DeferredRegister.create(Registries.MOB_EFFECT, TaCZTweaks.MOD_ID)

    @JvmField
    val ENDLESS_AMMO: DeferredHolder<MobEffect, EndlessAmmoStatusEffect> =
        EFFECTS.register("endless_ammo", Supplier { EndlessAmmoStatusEffect() })
}
