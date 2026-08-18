package me.muksc.tacztweaks.core

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IAmmoBox
import com.tacz.guns.api.item.IGun
import com.tacz.guns.resource.index.CommonGunIndex
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

object Context {
    class Gun(
        val stack: ItemStack?
    ) {
        val gun: IGun?
            get() = stack?.let { IGun.getIGunOrNull(it) }
        val id: Identifier?
            get() = stack?.let { gun?.getGunId(it) }
        val index: CommonGunIndex?
            get() = id?.let { TimelessAPI.getCommonGunIndex(it).getOrNull() }
    }

    fun Inventory.hasInfiniteAmmo(gunStack: ItemStack) = (0..containerSize).any { index ->
        val stack = getItem(index)
        val item = stack.item
        if (item !is IAmmoBox) return@any false
        if (!item.isAmmoBoxOfGun(gunStack, stack)) return@any false
        item.isAllTypeCreative(stack) || item.isCreative(stack)
    }
}