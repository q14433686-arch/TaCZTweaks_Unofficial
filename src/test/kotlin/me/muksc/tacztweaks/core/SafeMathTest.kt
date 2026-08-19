package me.muksc.tacztweaks.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SafeMathTest {
    @Test
    fun `armor ignore monotonically increases block progress`() {
        val withoutIgnore = SafeMath.blockBreakingDelta(9.0F, 0.0, 10.0F)
        val halfIgnore = SafeMath.blockBreakingDelta(9.0F, 0.5, 10.0F)
        val fullIgnore = SafeMath.blockBreakingDelta(9.0F, 1.0, 10.0F)

        assertTrue(withoutIgnore < halfIgnore)
        assertTrue(halfIgnore < fullIgnore)
    }

    @Test
    fun `zero hardness is immediate and invalid numbers fail closed`() {
        assertEquals(1.0F, SafeMath.blockBreakingDelta(0.0F, 0.0, 0.0F))
        assertEquals(0.0F, SafeMath.blockBreakingDelta(Float.NaN, 0.0, 1.0F))
        assertEquals(0.0F, SafeMath.blockBreakingDelta(1.0F, Double.NaN, 1.0F))
        assertEquals(0.0F, SafeMath.blockBreakingDelta(-1.0F, 0.0, 1.0F))
        assertEquals(0.0F, SafeMath.blockBreakingDelta(1.0F, 0.0, -1.0F))
    }
}
