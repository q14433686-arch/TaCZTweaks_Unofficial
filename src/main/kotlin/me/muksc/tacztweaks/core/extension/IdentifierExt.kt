package me.muksc.tacztweaks.core.extension

import me.muksc.tacztweaks.core.registry.PlatformRegistries
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
val Block.id: ResourceLocation? get() = PlatformRegistries.BLOCK.getKey(this)

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
val EntityType<*>.id: ResourceLocation? get() = PlatformRegistries.ENTITY_TYPE.getKey(this)