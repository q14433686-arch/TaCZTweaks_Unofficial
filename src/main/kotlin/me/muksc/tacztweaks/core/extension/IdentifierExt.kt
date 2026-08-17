package me.muksc.tacztweaks.core.extension

import me.muksc.tacztweaks.core.registry.PlatformRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block

val Block.id: Identifier? get() = PlatformRegistries.BLOCK.getKey(this)

val EntityType<*>.id: Identifier? get() = PlatformRegistries.ENTITY_TYPE.getKey(this)