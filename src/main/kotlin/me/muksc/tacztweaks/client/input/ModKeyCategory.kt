package me.muksc.tacztweaks.client.input

import me.muksc.tacztweaks.TaCZTweaks
//? if >=1.21.11 {
/*import net.minecraft.client.KeyMapping
*///?}

object ModKeyCategory {
    @JvmField
    val VALUE = /*? if >=1.21.11 {*/ /*KeyMapping.Category.register(TaCZTweaks.id("mod"))*/ /*?} else {*/ TaCZTweaks.keyCategory("mod") /*?}*/
}
