package me.muksc.tacztweaks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict TaCZ NeoForge 1.21.11 release-family validator.
 * <p>
 * Replaces the previous {@code String.startsWith} prefix check which would happily accept
 * any garbage text after the prefix (e.g. {@code 1.1.8+neoforge.1.21.11EVIL}) and could
 * not reliably distinguish the correct release family from Fabric builds, wrong MC lines
 * or malformed revision tags.
 * <p>
 * Accepted form (case-insensitive on the {@code r} marker):
 * <pre>
 *     1.1.8+neoforge.1.21.11.&lt;r|R&gt;&lt;digits&gt;[ -&lt;alphanum/dot pre-release&gt; ]...
 * </pre>
 * Examples:
 * <ul>
 *   <li>{@code 1.1.8+neoforge.1.21.11.r0}  — current shipped dependency</li>
 *   <li>{@code 1.1.8+neoforge.1.21.11.R1}  — forward-compatible with later revisions</li>
 *   <li>{@code 1.1.8+neoforge.1.21.11.R2}  — same release line</li>
 *   <li>{@code 1.1.8+neoforge.1.21.11.r10} — numeric compare, R10 &gt; R2 (no lex. trap)</li>
 *   <li>{@code 1.1.8+neoforge.1.21.11.R2-hotfix.1} — allowed pre-release/build suffix</li>
 * </ul>
 * Rejected: wrong core version (1.1.9), wrong platform/mc family (fabric, neoforge.26.2),
 * missing or malformed revision ({@code .r}, {@code .rx}, {@code .r-1}), Fabric builds,
 * null, and any "prefix + arbitrary junk" string.
 * <p>
 * This is intentionally <b>not</b> the Fabric-side rule of "revision &ge; R2":
 * the NeoForge 1.21.11 release line ships {@code r0} as the baseline, so any non-negative
 * revision in the matching core/platform/mc family is accepted.
 */
public final class TaczVersionSupport {

    /** Core TaCZ version this port is built against. */
    public static final String EXPECTED_CORE_VERSION = "1.1.8";

    /** Platform + Minecraft release family token after the '+', e.g. {@code neoforge.1.21.11}. */
    public static final String EXPECTED_FAMILY = "neoforge.1.21.11";

    /**
     * Full strict pattern. Group 1 captures the numeric revision so we can compare it as
     * an integer (avoids the "R10 &lt; R2" lexicographic trap).
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^" + Pattern.quote(EXPECTED_CORE_VERSION)
                    + "\\+" + Pattern.quote(EXPECTED_FAMILY)
                    + "\\.[rR](0|[1-9][0-9]*)"                 // revision: r0, R1, R10, ... (no leading zeros except r0)
                    + "(?:-[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*)*" // optional -pre / -hotfix.1 / ...
                    + "$"
    );

    /** Minimum accepted revision on the NeoForge 1.21.11 line. The shipped jar is r0. */
    private static final int MIN_REVISION = 0;

    private TaczVersionSupport() {
    }

    /**
     * @param version the raw {@code ModInfo.getVersion().toString()} for the {@code tacz} mod.
     * @return {@code true} iff the version belongs to the supported NeoForge 1.21.11 release family
     *         (core {@code 1.1.8}, platform {@code neoforge}, MC {@code 1.21.11}, well-formed revision
     *         &ge; r0, and an optional sane pre-release suffix).
     */
    public static boolean isSupportedTaczVersion(String version) {
        if (version == null) return false;
        Matcher m = PATTERN.matcher(version);
        if (!m.matches()) return false;
        int rev;
        try {
            rev = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return false;
        }
        return rev >= MIN_REVISION;
    }

    /** Human-readable expected version range, for error messages. */
    public static String expectedDisplay() {
        return EXPECTED_CORE_VERSION + "+" + EXPECTED_FAMILY + ".r<n>  (r0, R1, R2, ...; numeric compare)";
    }
}
