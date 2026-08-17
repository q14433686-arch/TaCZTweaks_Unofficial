package me.muksc.tacztweaks.core.registry

import com.mojang.datafixers.util.Either
import net.minecraft.core.Holder
import net.minecraft.core.HolderOwner
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import java.util.Optional
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

class DeferredHolder<R, T : R>(
    private val holder: Holder<R>
) : Holder<R>, Supplier<T> {
    @Suppress("UNCHECKED_CAST")
    override fun get(): T = holder.value() as T

    @Suppress("UNCHECKED_CAST")
    override fun value(): T = holder.value() as T

    override fun isBound(): Boolean = holder.isBound

    override fun `is`(location: Identifier): Boolean = holder.`is`(location)

    override fun `is`(resourceKey: ResourceKey<R>): Boolean = holder.`is`(resourceKey)

    override fun `is`(predicate: Predicate<ResourceKey<R>>): Boolean = holder.`is`(predicate)

    override fun `is`(tagKey: TagKey<R>): Boolean = holder.`is`(tagKey)

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun `is`(holder: Holder<R>): Boolean = holder.`is`(holder)

    override fun tags(): Stream<TagKey<R>> = holder.tags()

    override fun unwrap(): Either<ResourceKey<R>, R> = holder.unwrap()

    override fun unwrapKey(): Optional<ResourceKey<R>> = holder.unwrapKey()

    override fun kind(): Holder.Kind = holder.kind()

    override fun canSerializeIn(owner: HolderOwner<R>): Boolean = holder.canSerializeIn(owner)

    fun getDelegate(): Holder<R> = holder
}

fun <R, T : R> DeferredHolder<R, T>.wrap() = this
