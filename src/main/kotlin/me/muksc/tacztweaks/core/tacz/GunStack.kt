package me.muksc.tacztweaks.core.tacz

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IGun
import com.tacz.guns.resource.index.CommonGunIndex
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

@JvmInline
value class GunStack(val stack: ItemStack) {
    val gun: IGun? get() = IGun.getIGunOrNull(stack)
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    val id: ResourceLocation? get() = gun?.getGunId(stack)
    val index: CommonGunIndex? get() = TimelessAPI.getCommonGunIndex(id).getOrNull()
}