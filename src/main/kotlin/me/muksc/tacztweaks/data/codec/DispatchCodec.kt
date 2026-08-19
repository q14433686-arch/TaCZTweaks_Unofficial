package me.muksc.tacztweaks.data.codec

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec

/**
 * Dispatch helper for the `"type": "..."` discriminator field used by all data-driven
 * bullet interaction / sound / particle files.
 */
interface DispatchCodec<T> {
    val key: String
    val codecProvider: () -> Codec<out T>

    companion object {
        fun <T : DispatchCodec<*>> getCodec(valueOf: (String) -> T): Codec<T> = Codec.STRING.comapFlatMap({
            try {
                DataResult.success(valueOf(it))
            } catch (e: IllegalArgumentException) {
                DataResult.error { e.stackTraceToString() }
            }
        }, DispatchCodec<*>::key)
    }
}

/**
 * DFU 10 (bundled with Minecraft 26.1.2) changed [Codec.dispatch] to require a
 * [MapCodec] provider instead of a [Codec] provider. This extension keeps the 1.20-style
 * call shape (`dispatch(keyOf) { it.codecProvider() }`) by bridging through
 * [MapCodec.assumeMapUnsafe].
 */
fun <E, K> Codec<K>.dispatchBy(getKey: (E) -> K, getCodec: (K) -> Codec<out E>): Codec<E> =
    dispatch(getKey) { key -> MapCodec.assumeMapUnsafe(getCodec(key)) }
