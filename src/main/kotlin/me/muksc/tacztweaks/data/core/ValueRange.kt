package me.muksc.tacztweaks.data.core

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.muksc.tacztweaks.data.codec.strictOptionalFieldOf

class ValueRange(
    val min: Double,
    val max: Double
) : ClosedFloatingPointRange<Double> by min..max {
    companion object {
        // Double.MIN_VALUE is the smallest positive value, not the most negative value.
        val DEFAULT = ValueRange(-Double.MAX_VALUE, Double.MAX_VALUE)

        private val RAW_CODEC: Codec<ValueRange> = RecordCodecBuilder.create<ValueRange> { it.group(
            Codec.DOUBLE.strictOptionalFieldOf("min", DEFAULT.min).forGetter(ValueRange::min),
            Codec.DOUBLE.strictOptionalFieldOf("max", DEFAULT.max).forGetter(ValueRange::max)
        ).apply(it, ::ValueRange) }

        val CODEC: Codec<ValueRange> = RAW_CODEC.validate { range ->
            when {
                !range.min.isFinite() || !range.max.isFinite() ->
                    DataResult.error { "ValueRange bounds must be finite" }
                range.min > range.max ->
                    DataResult.error { "ValueRange min (${range.min}) exceeds max (${range.max})" }
                else -> DataResult.success(range)
            }
        }
    }
}
