package me.muksc.tacztweaks.compat.lrtactical

import me.xjqsh.lrtactical.api.item.IMeleeWeapon
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * LRTactical is bundled into the unofficial TaCZ port (`provides: ["lrtactical"]`),
 * so this is available whenever TaCZ itself is.
 */
object LRTacticalCompat {
    fun isEnabled(): Boolean = FabricLoader.getInstance().isModLoaded("lrtactical")
        || FabricLoader.getInstance().isModLoaded("tacz")

    fun getWeaponId(stack: ItemStack): Identifier? {
        if (!isEnabled()) return null
        return IMeleeWeapon.of(stack)?.getId(stack)
    }
}
