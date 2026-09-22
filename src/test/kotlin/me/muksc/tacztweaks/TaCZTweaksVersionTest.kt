package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the runtime TaCZ version gate in [TaCZTweaks.onInitialize].
 *
 * Context: on 2026-09-22 this gate still read `1.1.8+fabric.26.2.R2` on the 26.3 branch, so
 * the mod threw `IllegalStateException` during entrypoint init and the game could not start,
 * even though all four CI workflows were green. This test was part of the reason it went
 * unnoticed: it asserted the 26.2 values, including that `R1` must be *rejected* -- and R1
 * is precisely how 26.3 shipped. A test that pins the wrong expectation is worse than none.
 *
 * So the first test below asserts against the exact version this branch actually depends on,
 * derived from the single source of truth rather than retyped, so it cannot silently drift
 * out of sync with `fabric.mod.json` again.
 */
class TaCZTweaksVersionTest {
    @Test
    fun `accepts the exact TaCZ build this branch depends on`() {
        // If this fails, the runtime gate and the declared dependency disagree and users get
        // a hard crash at startup.
        assertTrue(
            TaCZTweaks.isSupportedTaczVersion(TaCZTweaks.SUPPORTED_TACZ_VERSION),
            "the runtime gate must accept its own declared SUPPORTED_TACZ_VERSION"
        )
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.3.R1"))
    }

    @Test
    fun `accepts hotfixes and later R revisions in the same family`() {
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.3.R1-hotfix"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.3.R2"))
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.3.R10"))
    }

    @Test
    fun `rejects other release families and malformed versions`() {
        // The whole point of the gate: a 26.2 jar must not satisfy the 26.3 branch.
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R2"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.2.R10"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.9+fabric.26.3.R1"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.26.3.RX"))
        assertFalse(TaCZTweaks.isSupportedTaczVersion(null))
    }
}
