package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaCZTweaksVersionTest {
    @Test
    fun `accepts the pinned R2 and R2 hotfix builds`() {
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R2"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R2-hotfix"))
    }

    @Test
    fun `rejects other TaCZ builds`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R1"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R3"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8"))
    }
}
