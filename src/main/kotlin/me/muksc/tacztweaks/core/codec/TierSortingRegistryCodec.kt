package me.muksc.tacztweaks.core.codec

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import me.muksc.tacztweaks.core.Identifier
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Tier
import net.minecraft.world.item.Tiers

val TierSortingRegistryCodec: Codec<Tier> = Identifier.CODEC.flatXmap({
    try {
        DataResult.success(Tiers.valueOf(it.path.uppercase()))
    } catch (e: IllegalArgumentException) {
        DataResult.error { e.message }
    }
}, {
    val tier = Tiers.entries.find { tier -> tier == it } ?: return@flatXmap DataResult.error { "Cannot serialize tier $it" }
    DataResult.success(Identifier.withDefaultNamespace(tier.name.lowercase()))
})
