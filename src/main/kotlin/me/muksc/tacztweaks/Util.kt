package me.muksc.tacztweaks

import com.google.common.collect.ImmutableMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block

fun <T> identity(value: T): T = value

inline fun <T> Collection<T>.anyOrEmpty(predicate: (T) -> Boolean): Boolean = isEmpty() || any(predicate)

fun <K : Any, V : Any> Map<K, V>.toImmutableMap(): ImmutableMap<K, V> = ImmutableMap.copyOf(this)

inline fun <T> Comparator<T>.thenPrioritizeBy(crossinline selector: (T) -> Boolean): Comparator<T> = thenByDescending(selector)

val Block.id: Identifier?
    get() = BuiltInRegistries.BLOCK.getKey(this)

val EntityType<*>.id: Identifier?
    get() = BuiltInRegistries.ENTITY_TYPE.getKey(this)
