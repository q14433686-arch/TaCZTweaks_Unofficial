package me.muksc.tacztweaks.core.codec

import com.mojang.serialization.Codec
import net.minecraft.advancements.critereon.BlockPredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders

val NumberProviderCodec: Codec<NumberProvider> = NumberProviders.CODEC

val IntsMinMaxBoundsCodec: Codec<MinMaxBounds.Ints> = MinMaxBounds.Ints.CODEC

val DoublesMinMaxBoundsCodec: Codec<MinMaxBounds.Doubles> = MinMaxBounds.Doubles.CODEC

val LootItemConditionCodec: Codec<LootItemCondition> = LootItemCondition.DIRECT_CODEC

val ItemPredicateCodec: Codec<ItemPredicate> = ItemPredicate.CODEC

val BlockPredicateCodec: Codec<BlockPredicate> = BlockPredicate.CODEC

val EntityPredicateCodec: Codec<EntityPredicate> = EntityPredicate.CODEC
