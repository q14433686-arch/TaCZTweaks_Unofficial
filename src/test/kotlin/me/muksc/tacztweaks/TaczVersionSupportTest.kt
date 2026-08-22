package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the runtime TaCZ version gate for the NeoForge 26.1.2 release family.
 *
 * The gate accepts `1.1.8+neoforge.26.1.2.R<n>` with `n >= 1` (TaCZ: Renovated ships R1),
 * optionally followed by a legal suffix such as `-hotfix.1`. Fabric strings, other Minecraft
 * families, wrong core versions, pre-R1 revisions and malformed strings must be rejected.
 * The revision is compared numerically so `R10` is not mistakenly seen as lower than `R2`.
 */
class TaczVersionSupportTest {

    @Test
    fun `accepts R1 and later revisions of the NeoForge 26_1_2 family`() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R1"))
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.r1"))
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R2"))
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R10"))
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R1-hotfix"))
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R1-hotfix.1"))
    }

    @Test
    fun `rejects the pre-R1 revision of this line`() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.r0"))
    }

    @Test
    fun `rejects other loaders and Minecraft families`() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+fabric.26.1.2.R2"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r0"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.2.R1"))
    }

    @Test
    fun `rejects wrong core versions`() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.9+neoforge.26.1.2.R1"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("2.0.0+neoforge.26.1.2.R1"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8"))
    }

    @Test
    fun `rejects missing or malformed revisions`() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.Rx"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R-1"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R01"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R1hotfix"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2.R1/invalid"))
    }

    @Test
    fun `rejects prefix plus junk, null and empty`() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.1.2EVIL"))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion(null))
        assertFalse(TaczVersionSupport.isSupportedTaczVersion(""))
    }
}
