package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards the runtime TaCZ version gate in [TaCZTweaks.onInitialize].
 *
 * Context (synced from the `26.3` branch): on 2026-09-22 the gate there still read
 * `1.1.8+fabric.26.2.R2`, so the mod threw `IllegalStateException` during entrypoint init
 * and the game could not start, even though every CI workflow was green. This test was part
 * of why it went unnoticed: it asserted the previous line's values, including that `R1`
 * must be *rejected* — and R1 is precisely how 26.3 shipped. A test that pins the wrong
 * expectation is worse than no test.
 *
 * So the first assertion below evaluates [TaCZTweaks.SUPPORTED_TACZ_VERSION] — the single
 * source of truth — instead of a retyped literal, and cannot silently drift out of sync
 * with `fabric.mod.json`. `scripts/check_release_consistency.py::check_runtime_version_gate`
 * cross-checks the same pair from the Python side.
 *
 * Branch-specific: this 1.21.11 line floors at **R2**, because TaCZ Refabricated 1.21.11 R1
 * predates APIs this mod needs. 26.3 floors at R1. Do not copy one line's floor to the
 * other — see `docs/CROSS_BRANCH_SYNC.md` §0.3 / §3.2 on the `26.3` branch.
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
        assertTrue(TaCZTweaks.isSupportedTaczVersion("1.1.8+fabric.1.21.11.R2"))
    }

    @Test
    fun `accepts hotfixes and later R revisions in the same family`() {
        // Revision comparison is numeric, so R10 must rank above R2 rather than below it.
        listOf(
            "1.1.8+fabric.1.21.11.R2-hotfix",
            "1.1.8+fabric.1.21.11.R3",
            "1.1.8+fabric.1.21.11.R10",
            "1.1.8+fabric.1.21.11.R10-preview",
        ).forEach { version -> assertTrue(TaCZTweaks.isSupportedTaczVersion(version), version) }
    }

    @Test
    fun `rejects revisions below the R2 floor on this line`() {
        // Deliberate branch difference: 26.3 must ACCEPT its R1; this line must reject R1.
        listOf(
            "1.1.8+fabric.1.21.11.R1",
            "1.1.8+fabric.1.21.11.R1-hotfix",
            "1.1.8+fabric.1.21.11.R0",
        ).forEach { version -> assertFalse(TaCZTweaks.isSupportedTaczVersion(version), version) }
    }

    @Test
    fun `rejects other release families and malformed versions`() {
        // The whole point of the gate: a jar built for another Minecraft/release family
        // must not satisfy this branch. These are wrong-family cases, not "previous line"
        // expectations — unlike the 26.3 incident, they stay correct after a version bump.
        listOf(
            "1.1.8",
            "1.1.9+fabric.1.21.11.R2",
            "1.1.8+fabric.26.1.2.R2",
            "1.1.8+fabric.26.2.R2",
            "1.1.8+fabric.26.3.R1",
            "1.1.8+fabric.1.21.11.R",
            "1.1.8+fabric.1.21.11.RX",
            "1.1.8+fabric.1.21.11.R2/invalid",
            null,
        ).forEach { version -> assertFalse(TaCZTweaks.isSupportedTaczVersion(version), version) }
    }
}
