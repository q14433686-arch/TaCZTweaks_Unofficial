package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The runtime gate must accept its own declared 26.2/R2 baseline, not 26.3's R1. */
class TaCZTweaksVersionTest {
    @Test
    fun `accepts the exact TaCZ build this branch depends on`() {
        assertTrue(TaCZTweaks.isSupportedTaczVersion(TaCZTweaks.SUPPORTED_TACZ_VERSION))
    }

    @Test
    fun `accepts R2 hotfixes and later revisions in the same family`() {
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R2-hotfix"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R3"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R10"))
    }

    @Test
    fun `rejects pre R2 and unrelated TaCZ builds`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R1"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R1-hotfix"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.3.R1"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.9+fabric.26.2.R2"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion(null))
    }
}
