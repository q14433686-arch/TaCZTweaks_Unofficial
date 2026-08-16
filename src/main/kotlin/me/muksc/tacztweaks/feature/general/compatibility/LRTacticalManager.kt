package me.muksc.tacztweaks.feature.general.compatibility

import me.muksc.tacztweaks.core.compatibility.ModCompatibilityManager
import me.muksc.tacztweaks.core.modVersionRange
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

//? if forge || neoforge || (fabric && >=1.21.11)
import me.xjqsh.lrtactical.api.item.IMeleeWeapon

object LRTacticalManager : ModCompatibilityManager(
    modId = "lrtactical",
    mixinPackagePrefix = "me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical"
) {
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    fun getWeaponId(stack: ItemStack): ResourceLocation? = withFallback(null) {
        Inner.getWeaponId(stack)
    }

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = when (mixinClassName) {
        "me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical.BuiltinMeleeWeaponMixin" -> true
        // The unofficial port provides lrtactical with TaCZ's 1.1.8 version. Its melee logic is
        // an IMeleeWeapon default method, not either standalone 0.3/0.4 MeleeItem lambda.
        //? if fabric && >=1.21.11 {
        /*"me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical.MeleeItemMixin_0_4_0",
        "me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical.MeleeItemMixin_0_3_0" -> false
        *///?} else {
        "me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical.MeleeItemMixin_0_4_0" -> modVersionRange(modId, "0.4.0")
        "me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical.MeleeItemMixin_0_3_0" -> modVersionRange(modId, "0.3.0", "0.4.0")
        //?}
        else -> true
    }

    private object Inner {
        //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
        fun getWeaponId(stack: ItemStack): ResourceLocation? =
            /*? if forge || neoforge || (fabric && >=1.21.11) {*/ IMeleeWeapon.of(stack)?.getId(stack) /*?} else {*/ /*null *//*?}*/
    }
}
