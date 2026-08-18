package me.muksc.tacztweaks.data.codec

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import me.muksc.tacztweaks.identity

/** Accepts either a single object or a list of objects, normalizing to a list. */
fun <T> singleOrListCodec(codec: Codec<T>): Codec<List<T>> =
    Codec.either(codec, Codec.list(codec))
        .xmap({ it.map(::listOf, ::identity) }, { when {
            it.size == 1 -> Either.left(it.first())
            else -> Either.right(it)
        } })

fun <T, R : Comparable<R>> Codec<List<T>>.sortedBy(selector: (T) -> R): Codec<List<T>> =
    xmap(::identity) { it.sortedBy(selector) }
