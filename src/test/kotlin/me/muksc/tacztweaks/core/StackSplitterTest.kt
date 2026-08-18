package me.muksc.tacztweaks.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StackSplitterTest {
    @Test
    fun `splits exact and partial stacks without zero entries`() {
        assertEquals(listOf(64, 64), StackSplitter.split(128, 64))
        assertEquals(listOf(64, 64, 2), StackSplitter.split(130, 64))
        assertEquals(emptyList<Int>(), StackSplitter.split(0, 64))
    }

    @Test
    fun `rejects malformed counts and stack sizes`() {
        assertNull(StackSplitter.split(-1, 64))
        assertNull(StackSplitter.split(StackSplitter.MAX_UNLOAD_AMMO + 1, 64))
        assertNull(StackSplitter.split(10, 0))
        assertNull(StackSplitter.split(10, -1))
        assertNull(StackSplitter.split(10, StackSplitter.MAX_STACK_SIZE + 1))
    }
}
