package me.muksc.tacztweaks.core.codec

//? if forge {
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Tier
import net.minecraftforge.common.TierSortingRegistry

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
val TierSortingRegistryCodec: Codec<Tier> = ResourceLocation.CODEC.flatXmap({
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    TierSortingRegistry.byName(it)?.let(DataResult<ResourceLocation>::success) ?: DataResult.error {
        "Unknown registry key in TierSortingRegistry: $it"
    }
}, {
    TierSortingRegistry.getName(it)?.let(DataResult<Tier>::success) ?: DataResult.error {
        "Unknown registry element in TierSortingRegistry: $it"
    }
})
//?} else if >=1.21.11 {
/*import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import me.muksc.tacztweaks.core.Identifier
import net.minecraft.resources.Identifier as MinecraftIdentifier
import net.minecraft.world.item.ToolMaterial

private val TOOL_MATERIALS = mapOf(
    "wood" to ToolMaterial.WOOD,
    "stone" to ToolMaterial.STONE,
    "copper" to ToolMaterial.COPPER,
    "iron" to ToolMaterial.IRON,
    "diamond" to ToolMaterial.DIAMOND,
    "gold" to ToolMaterial.GOLD,
    "netherite" to ToolMaterial.NETHERITE
)

val TierSortingRegistryCodec: Codec<ToolMaterial> = MinecraftIdentifier.CODEC.flatXmap({ id ->
    TOOL_MATERIALS[id.path]?.let(DataResult<ToolMaterial>::success)
        ?: DataResult.error { "Unknown vanilla tool material: $id" }
}, { material ->
    TOOL_MATERIALS.entries.firstOrNull { it.value == material }
        ?.let { DataResult.success(Identifier(it.key)) }
        ?: DataResult.error { "Cannot serialize tool material $material" }
})
*///?} else {
/*import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import me.muksc.tacztweaks.core.Identifier
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Tier
import net.minecraft.world.item.Tiers

val TierSortingRegistryCodec: Codec<Tier> = ResourceLocation.CODEC.flatXmap({
    try {
        DataResult.success(Tiers.valueOf(it.path.uppercase()))
    } catch (e: IllegalArgumentException) {
        DataResult.error { e.message }
    }
}, {
    val tier = Tiers.entries.find { tier -> tier == it } ?: return@flatXmap DataResult.error { "Cannot serialize tier $it" }
    DataResult.success(Identifier(tier.name.lowercase()))
})
*///?}
