package me.muksc.tacztweaks.compat.lrtactical

import me.xjqsh.lrtactical.api.item.IMeleeWeapon
import net.neoforged.fml.ModList
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * TaCZ: Renovated carries the LRTactical layer inside its own jar (`me.xjqsh.lrtactical.*`),
 * so these classes are on the classpath whenever TaCZ is. A separate `lrtactical` mod id is
 * still accepted in case the layer is ever published standalone.
 */
object LRTacticalCompat {
    fun isEnabled(): Boolean = ModList.get().isLoaded("lrtactical")
        || ModList.get().isLoaded("tacz")

    fun getWeaponId(stack: ItemStack): Identifier? {
        if (!isEnabled()) return null
        return IMeleeWeapon.of(stack)?.getId(stack)
    }
}
