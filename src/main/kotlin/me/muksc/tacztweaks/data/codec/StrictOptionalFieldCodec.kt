package me.muksc.tacztweaks.data.codec

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import java.util.Optional
import java.util.stream.Stream

/**
 * An optional-field codec that still reports parse errors inside the value instead of
 * swallowing them (unlike [Codec.optionalFieldOf] which is lenient).
 */
class StrictOptionalFieldCodec<A : Any>(
    private val name: String,
    private val elementCodec: Codec<A>
) : MapCodec<Optional<A>>() {
    override fun <T : Any> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<Optional<A>> {
        val value = input.get(name) ?: return DataResult.success(Optional.empty())
        val parsed = elementCodec.parse(ops, value)
        return parsed.map(Optional<A>::of).setPartial(parsed.resultOrPartial { })
    }

    override fun <T : Any> encode(input: Optional<A>, ops: DynamicOps<T>, prefix: RecordBuilder<T>): RecordBuilder<T> {
        if (input.isPresent) return prefix.add(name, elementCodec.encodeStart(ops, input.get()))
        return prefix
    }

    override fun <T : Any> keys(ops: DynamicOps<T>): Stream<T> =
        Stream.of(ops.createString(name))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StrictOptionalFieldCodec<*>) return false
        return name == other.name && elementCodec == other.elementCodec
    }

    override fun hashCode(): Int =
        java.util.Objects.hash(name, elementCodec)

    override fun toString(): String =
        "StrictOptionalFieldCodec[$name: $elementCodec]"
}

fun <T : Any> Codec<T>.strictOptionalFieldOf(name: String): MapCodec<Optional<T>> =
    StrictOptionalFieldCodec(name, this)

fun <T : Any> Codec<T>.strictOptionalFieldOf(name: String, defaultValue: T): MapCodec<T> =
    StrictOptionalFieldCodec(name, this).xmap(
        { it.orElse(defaultValue) },
        { if (it == defaultValue) Optional.empty() else Optional.of(it) }
    )
