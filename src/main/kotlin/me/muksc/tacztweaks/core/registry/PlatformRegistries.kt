package me.muksc.tacztweaks.core.registry

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.level.block.Block

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries

object PlatformRegistries {
    val BLOCK: Registry<Block> = BuiltInRegistries.BLOCK
    val ATTRIBUTE: Registry<Attribute> = BuiltInRegistries.ATTRIBUTE
    val ENTITY_TYPE: Registry<EntityType<*>> = BuiltInRegistries.ENTITY_TYPE
    val MOB_EFFECT: Registry<MobEffect> = BuiltInRegistries.MOB_EFFECT
}
