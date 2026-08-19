package me.muksc.tacztweaks.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectileIndexAllocatorTest {
    @Test
    fun `indexes pellets per successful burst cycle`() {
        val indices = ProjectileIndexAllocator()
        indices.resetShot()

        indices.beginCycle()
        assertEquals(0, indices.burstIndex)
        assertEquals(listOf(0, 1, 2), List(3) { indices.takePelletIndex() })
        indices.completeCycle(true)

        indices.beginCycle()
        assertEquals(1, indices.burstIndex)
        assertEquals(listOf(0, 1), List(2) { indices.takePelletIndex() })
        indices.completeCycle(false)
        assertEquals(1, indices.burstIndex)
    }
}
