package me.muksc.tacztweaks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict TaCZ NeoForge 26.2 release-family validator.
 *
 * <p>The target dependency is
 * <a href="https://github.com/q14433686-arch/TaCZ_Renovated">TaCZ: Renovated</a>.
 * Its {@code 26.2} branch first publishes
 * {@code mod_version=1.1.8+neoforge.26.2.R1}. A prefix check would accept malformed suffixes
 * and could not distinguish Fabric, 1.21.11, or 26.1.2 builds, so this validator matches the
 * complete friendly version and compares the revision numerically.</p>
 *
 * <p>Accepted form (case-insensitive on the {@code r} marker):</p>
 * <pre>
 *     1.1.8+neoforge.26.2.&lt;r|R&gt;&lt;digits&gt;[-&lt;alphanumeric/dot suffix&gt;]...
 * </pre>
 *
 * <p>The minimum is R1. This is deliberately different from the Fabric 26.2 sibling's R2
 * minimum and the NeoForge 1.21.11 line's r0 minimum.</p>
 */
public final class TaczVersionSupport {

    /** Core TaCZ version this port is built against. */
    public static final String EXPECTED_CORE_VERSION = "1.1.8";

    /** Platform and Minecraft release-family token after the '+'. */
    public static final String EXPECTED_FAMILY = "neoforge.26.2";

    private static final Pattern PATTERN = Pattern.compile(
            "^" + Pattern.quote(EXPECTED_CORE_VERSION)
                    + "\\+" + Pattern.quote(EXPECTED_FAMILY)
                    + "\\.[rR](0|[1-9][0-9]*)"
                    + "(?:-[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*)*"
                    + "$"
    );

    /** TaCZ: Renovated 26.2 starts at R1. */
    private static final int MIN_REVISION = 1;

    private TaczVersionSupport() {
    }

    /**
     * @return {@code true} only for core 1.1.8, NeoForge 26.2, a well-formed revision at least
     *         R1, and an optional well-formed suffix.
     */
    public static boolean isSupportedTaczVersion(String version) {
        if (version == null) return false;
        Matcher matcher = PATTERN.matcher(version);
        if (!matcher.matches()) return false;
        try {
            return Integer.parseInt(matcher.group(1)) >= MIN_REVISION;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Human-readable expected version range for startup diagnostics. */
    public static String expectedDisplay() {
        return EXPECTED_CORE_VERSION + "+" + EXPECTED_FAMILY
                + ".R<n> (R1 or later; revision compared numerically)";
    }
}
