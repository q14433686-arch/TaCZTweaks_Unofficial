package me.muksc.tacztweaks.core

import kotlin.math.exp

/** Pure, bounded arithmetic used at data-pack and network trust boundaries. */
object SafeMath {
    /**
     * Converts virtual bullet/melee damage to one tick of vanilla-style block breaking.
     *
     * Armor ignore reduces effective hardness, so increasing it can never make a block
     * harder to break. Invalid values fail closed instead of propagating NaN/Infinity into
     * the persistent breaking-progress map.
     */
    @JvmStatic
    fun blockBreakingDelta(damage: Float, armorIgnore: Double, hardness: Float): Float {
        if (!damage.isFinite() || damage < 0.0F) return 0.0F
        if (!armorIgnore.isFinite()) return 0.0F
        if (!hardness.isFinite() || hardness < 0.0F) return 0.0F
        if (hardness == 0.0F) return 1.0F

        val clampedArmorIgnore = armorIgnore.coerceIn(0.0, 1.0)
        val effectiveHardness = hardness.toDouble() * exp(-2.0 * clampedArmorIgnore)
        val delta = (1.0 + damage.toDouble()) / (effectiveHardness * 30.0)
        if (!delta.isFinite()) return if (delta > 0.0) 1.0F else 0.0F
        return delta.coerceIn(0.0, 1.0).toFloat()
    }
}
