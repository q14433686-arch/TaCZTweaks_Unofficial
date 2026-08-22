package me.muksc.tacztweaks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaczVersionSupport} — the NeoForge 1.21.11 release-family gate.
 * <p>
 * These tests intentionally do <b>not</b> boot Minecraft/NeoForge: the validator is pure-JDK
 * so {@code ./gradlew test} exercises it without any game runtime.
 * <p>
 * Acceptance contract:
 * <ul>
 *   <li>Accept: r0, R1, R2 (current shipped jar is r0; R1/R2 forward-compatible).</li>
 *   <li>Accept: sane pre-release suffix (e.g. {@code -hotfix.1}) on a valid family.</li>
 *   <li>Reject: null, wrong core version, wrong MC family, Fabric strings, malformed revision,
 *       "prefix + garbage".</li>
 *   <li>Revisions are compared numerically so r10 &gt; r2 (no lexicographic trap).</li>
 * </ul>
 */
class TaczVersionSupportTest {

    // ---- accept cases ----

    @Test
    void acceptsShippedRevision_r0() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r0"),
                "shipped libs/tacz-1.1.8+neoforge.1.21.11.r0.jar MUST be accepted");
    }

    @Test
    void acceptsCapitalR1() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.R1"));
    }

    @Test
    void acceptsCapitalR2() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.R2"));
    }

    @Test
    void acceptsR10_numericCompare() {
        // Guard against the R10 < R2 lexicographic trap.
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.R10"));
    }

    @Test
    void acceptsLowerCaseLargeRevision() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r99"));
    }

    @Test
    void acceptsHotfixSuffix() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.R2-hotfix.1"),
                "reasonable hotfix/preview suffix on the right family must be accepted");
    }

    @Test
    void acceptsBetaPreSuffix() {
        assertTrue(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r0-beta"));
    }

    // ---- reject cases ----

    @Test
    void rejectsNull() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion(null));
    }

    @Test
    void rejectsEmpty() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion(""));
    }

    @Test
    void rejectsWrongCoreVersion_119() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.9+neoforge.1.21.11.R2"),
                "wrong core TaCZ version must be rejected");
    }

    @Test
    void rejectsWrongCoreVersion_120() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.2.0+neoforge.1.21.11.r0"));
    }

    @Test
    void rejectsWrongMCFamily_neoforge262() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.26.2.R2"),
                "wrong MC/NeoForge release family (26.2) must be rejected");
    }

    @Test
    void rejectsFabricString() {
        // Typical Fabric-side TaCZ version string.
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+fabric.1.21.11.R2"),
                "Fabric platform strings must be rejected");
    }

    @Test
    void rejectsFabricFamilyAlt() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+1.21.11-fabric.R2"));
    }

    @Test
    void rejectsMissingRevision() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11"),
                "revision suffix is mandatory");
    }

    @Test
    void rejectsBareRMarker() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r"));
    }

    @Test
    void rejectsNonNumericRevision() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.rx"));
    }

    @Test
    void rejectsNegativeRevision() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r-1"));
    }

    @Test
    void rejectsLeadingZeroRevision() {
        // disallow r01 etc. to keep parsing unambiguous; r0 alone is accepted.
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r01"));
    }

    @Test
    void rejectsGarbageAfterPrefix() {
        // The exact regression the new gate is meant to block.
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11EVIL"),
                "'prefix + arbitrary junk' must not pass the gate");
    }

    @Test
    void rejectsGarbageAfterRevision() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8+neoforge.1.21.11.r0!!!"));
    }

    @Test
    void rejectsWrongPlatformSeparator() {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion("1.1.8-neoforge.1.21.11.r0"),
                "'-' instead of '+' between core and family is malformed");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.1.8+neoforge.1.21.11.r0 ",   // trailing space
            " 1.1.8+neoforge.1.21.11.r0",   // leading space
            "1.1.8+neoforge.1.21.11.R2;1",  // injected extra version
            "v1.1.8+neoforge.1.21.11.r0",   // leading 'v'
    })
    void rejectsIncorrectlyFormatted(String bad) {
        assertFalse(TaczVersionSupport.isSupportedTaczVersion(bad));
    }

    // ---- display helper ----

    @Test
    void expectedDisplayMentionsFamily() {
        String display = TaczVersionSupport.expectedDisplay();
        assertTrue(display.contains("1.1.8+neoforge.1.21.11"),
                "error message should carry the family token");
        assertTrue(display.contains("r0"), "error message should hint r0 is acceptable");
    }
}
