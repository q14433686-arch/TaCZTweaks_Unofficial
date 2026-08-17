package me.muksc.tacztweaks.feature.datapack.legacy.core

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.muksc.tacztweaks.core.codec.DispatchCodec
import me.muksc.tacztweaks.core.codec.EntityPredicateCodec
import me.muksc.tacztweaks.core.extension.id
import me.muksc.tacztweaks.core.registry.PlatformRegistries
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.util.StringRepresentable
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import kotlin.ranges.contains
import net.minecraft.world.entity.Entity as MCEntity

