package me.muksc.tacztweaks.core.codec

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import java.util.Optional

fun <T : Any> Codec<T>.strictOptionalFieldOf(name: String): MapCodec<Optional<T>> =
    optionalFieldOf(name)

fun <T : Any> Codec<T>.strictOptionalFieldOf(name: String, defaultValue: T): MapCodec<T> =
    optionalFieldOf(name, defaultValue)
