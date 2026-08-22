package me.muksc.tacztweaks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict TaCZ NeoForge 26.1.2 release-family validator.
 * <p>
 * The target dependency is <a href="https://github.com/q14433686-arch/TaCZ_Renovated">TaCZ:
 * Renovated</a>, whose {@code 26.1.2} branch publishes {@code mod_version=1.1.8+neoforge.26.1.2.R1}.
 * A plain {@code String.startsWith} prefix check would accept any garbage suffix
 * (e.g. {@code 1.1.8+neoforge.26.1.2EVIL}) and could not distinguish Fabric builds, the
 * 1.21.11 / 26.2 lines, or malformed revision tags, so the check is a full regex plus a
 * numeric revision comparison.
 * <p>
 * Accepted form (case-insensitive on the {@code r} marker):
 * <pre>
 *     1.1.8+neoforge.26.1.2.&lt;r|R&gt;&lt;digits&gt;[ -&lt;alphanum/dot suffix&gt; ]...
 * </pre>
 * Examples:
 * <ul>
 *   <li>{@code 1.1.8+neoforge.26.1.2.R1}  — current shipped dependency</li>
 *   <li>{@code 1.1.8+neoforge.26.1.2.R2}  — later revision, same line</li>
 *   <li>{@code 1.1.8+neoforge.26.1.2.r10} — numeric compare, R10 &gt; R2 (no lexicographic trap)</li>
 *   <li>{@code 1.1.8+neoforge.26.1.2.R1-hotfix.1} — allowed suffix</li>
 * </ul>
 * Rejected: {@code null}, empty, wrong core version ({@code 1.1.9}), wrong platform
 * ({@code fabric}), wrong Minecraft family ({@code 1.21.11}, {@code 26.2}), pre-R1
 * revisions ({@code r0}), malformed revisions ({@code .r}, {@code .rx}, {@code .r-1},
 * {@code .r01}) and any "prefix + arbitrary junk" string.
 * <p>
 * Unlike the NeoForge 1.21.11 line (which ships {@code r0} as its baseline), the 26.1.2
 * line's first release is {@code R1}, so {@code R1} is the minimum accepted revision here.
 * The Fabric sibling's "&ge; R2" rule does not apply either — copying it would reject the
 * only existing NeoForge 26.1.2 release.
 */
public final class TaczVersionSupport {

    /** Core TaCZ version this port is built against. */
    public static final String EXPECTED_CORE_VERSION = "1.1.8";

    /** Platform + Minecraft release family token after the '+', e.g. {@code neoforge.26.1.2}. */
    public static final String EXPECTED_FAMILY = "neoforge.26.1.2";

    /**
     * Full strict pattern. Group 1 captures the numeric revision so it can be compared as an
     * integer (avoids the "R10 &lt; R2" lexicographic trap).
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^" + Pattern.quote(EXPECTED_CORE_VERSION)
                    + "\\+" + Pattern.quote(EXPECTED_FAMILY)
                    + "\\.[rR](0|[1-9][0-9]*)"                 // revision: r0, R1, R10, ... (no leading zeros)
                    + "(?:-[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*)*" // optional -hotfix.1 / -preview / ...
                    + "$"
    );

    /** Minimum accepted revision on the NeoForge 26.1.2 line. TaCZ: Renovated ships R1. */
    private static final int MIN_REVISION = 1;

    private TaczVersionSupport() {
    }

    /**
     * @param version the raw {@code ModInfo.getVersion().toString()} for the {@code tacz} mod.
     * @return {@code true} iff the version belongs to the supported NeoForge 26.1.2 release family
     *         (core {@code 1.1.8}, platform {@code neoforge}, MC {@code 26.1.2}, well-formed
     *         revision &ge; R1, plus an optional sane suffix).
     */
    public static boolean isSupportedTaczVersion(String version) {
        if (version == null) return false;
        Matcher m = PATTERN.matcher(version);
        if (!m.matches()) return false;
        int revision;
        try {
            revision = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return false;
        }
        return revision >= MIN_REVISION;
    }

    /** Human-readable expected version range, for error messages. */
    public static String expectedDisplay() {
        return EXPECTED_CORE_VERSION + "+" + EXPECTED_FAMILY + ".R<n>  (R1, R2, R10, ...; numeric compare)";
    }
}
