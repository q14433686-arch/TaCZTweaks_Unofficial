package me.muksc.tacztweaks.compat.lrtactical

import me.xjqsh.lrtactical.api.item.IMeleeWeapon
import net.neoforged.fml.ModList
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * TaCZ 1.21.11 R2 advertises/embeds the LRTactical API surface, so this helper may resolve
 * melee weapon ids either from a standalone lrtactical mod or from TaCZ's provided classes.
 */
object LRTacticalCompat {
    fun isEnabled(): Boolean = ModList.get().isLoaded("lrtactical") ||
        ModList.get().isLoaded("tacz")

    fun getWeaponId(stack: ItemStack): Identifier? {
        if (!isEnabled()) return null
        return IMeleeWeapon.of(stack)?.getId(stack)
    }
}
