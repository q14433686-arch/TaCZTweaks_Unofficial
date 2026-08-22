package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the runtime TaCZ version gate for the current Minecraft release family.
 *
 * The gate accepts any `1.1.8+fabric.26.1.2.R<n>` where `n >= 2`, optionally followed by a
 * legal build suffix such as `-hotfix` or `-preview`. Wrong Minecraft / TaCZ core / Fabric
 * release-family versions, pre-R2 revisions and malformed strings must be rejected. The
 * revision is compared numerically so `R10` is not mistakenly seen as lower than `R2`.
 */
class TaCZTweaksVersionTest {

    @Test
    fun `accepts R2 and later revisions in the current release family`() {
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R2"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R2-hotfix"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R3"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R3-hotfix"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R10"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R10-preview"))
    }

    @Test
    fun `accepts a bare R2 minimum as the documented baseline`() {
        assertTrue(TaCZTweaks.isSupportedTaczVersion(TaCZTweaks.SUPPORTED_TACZ_VERSION))
    }

    @Test
    fun `rejects pre-R2 revisions in the current release family`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R1"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R1-hotfix"))
    }

    @Test
    fun `rejects a version without the revision marker`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2"))
    }

    @Test
    fun `rejects malformed revision suffix`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R2/invalid"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R2hotfix"))
    }

    @Test
    fun `rejects a bare core version without the fabric family`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8"))
    }

    @Test
    fun `rejects a higher TaCZ core version`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.9+fabric.26.1.2.R2"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("2.0.0+fabric.26.1.2.R2"))
    }

    @Test
    fun `rejects a different fabric release family`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.1.21.11.R2"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R2"))
    }

    @Test
    fun `rejects null`() {
        assertFalse(TaCZTweaks.isSupportedTaczVersion(null))
        assertFalse(TaCZTweaks.isSupportedTaczVersion(""))
    }
}
