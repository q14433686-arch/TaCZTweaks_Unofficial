package me.muksc.tacztweaks.compat.lrtactical

import me.xjqsh.lrtactical.api.item.IMeleeWeapon
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * LRTactical is bundled inside the unofficial TaCZ jar (`provides: ["lrtactical"]`),
 * so this is always available — no optional-mod gate needed.
 */
object LRTacticalCompat {
    fun getWeaponId(stack: ItemStack): Identifier? =
        IMeleeWeapon.of(stack)?.getId(stack)
}
