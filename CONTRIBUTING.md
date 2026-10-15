# Contributing

Thank you for helping maintain this unofficial Fabric 1.21.11 port. Arena sessions on this
checkout are pinned to a per-session `arena/<session-id>` branch. Normal GitHub work should use
feature branches and pull requests against the maintained `1.21.11` release branch.

## Development environment

- JDK 21 (not 25; that requirement belongs to the 26.x unobfuscated branches)
- Python 3
- Gradle Wrapper from this repository
- The exact vendored jars declared in `RESOURCE_IMPORT_MANIFEST.tsv`
- This branch uses `fabric-loom-remap`, official Mojang mappings, and `tacztweaks.refmap.json`

Useful commands:

```bash
python3 scripts/download_dependencies.py
python3 scripts/download_dependencies.py --check-only
# Offline-safe: absent libs/*.jar rows print SKIP instead of failing.
python3 scripts/check_release_consistency.py
# Same, but demanding the vendored jars (run after download_dependencies.py).
python3 scripts/check_release_consistency.py --require-deps
# Asks Modrinth whether the declared ranges match anything real for 1.21.11.
python3 scripts/check_dependency_availability.py
python3 -m pytest scripts/test_dependency_ranges.py -q
python3 scripts/check_mod_icon.py
python3 scripts/audit_port.py --strict \
  --tacz-jar libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar \
  --minecraft-named-jar <1.21.11 named jar> \
  --minecraft-intermediary-jar <1.21.11 intermediary jar> \
  --refmap build/resources/main/tacztweaks.refmap.json
./gradlew clean build --stacktrace
```

### Runtime-only constraints

`fabric.mod.json`'s `depends` ranges and the TaCZ version gate hardcoded in `TaCZTweaks.java`
are read by **nothing** in the build: not javac, not the mixin audit, not jar packaging. Only
the Fabric Loader evaluates them, when the game starts. A mistake there is invisible until a
user reports "all CI green, game won't launch". The `26.3` line hit that three times; the gates
above exist because of it. See `docs/CROSS_BRANCH_SYNC.md` on the `26.3` branch for the full
work order, including the two things you must **not** copy across lines:

- **Do not** copy 26.3's `fabric-language-kotlin` range (`>=1.14.1+kotlin.2.4.20`) here. On
  1.21.11 the correct range is `>=1.13.13 <1.14.0`; 26.3 only moved because 1.14.1 is the sole
  FLK build tagged for that game version.
- **Do not** copy 26.3's TaCZ revision floor (R1). This line requires **R2**, because TaCZ
  Refabricated 1.21.11 R1 predates APIs this mod needs.

## Pull request expectations

- Do not add unlicensed assets, copied code, logs with private data, or mutable binary blobs.
- Update `RESOURCE_IMPORT_MANIFEST.tsv`, `LICENSES.md`, and `THIRD_PARTY_NOTICES.md` when changing
  vendored binaries, embedded libraries or redistributed resources.
- Update docs and `CHANGELOG.md` for user-visible behavior changes.
- Keep `fabric.mod.json`, `gradle.properties`, README dependency tables and publish docs aligned.
- Never put an artificial upper bound on `fabric-api` (`>=<built version>` only); the Minecraft
  pin is what scopes the line. `check_release_consistency.py` rejects a cap.
- Changes under `.github/workflows/` need a token with `workflows` permission. If a push is
  refused with `refusing to allow a GitHub App to create or update workflow`, hand the file to a
  human to commit from the web UI instead of retrying.
- For mixins, cite the target class/method descriptors, keep remap/refmap notes accurate, and run
  the strict audit with the 1.21.11 jars.
- Do not claim game, dedicated-server, or optional-mod validation unless you actually ran it.
- Include or update tests for pure logic, codecs, config serialization or release packaging when
  feasible.

## Optional compatibility evidence

New compatibility code should include the exact mod name, version, loader, Minecraft version,
source or binary inspected, test scenario, and known boundaries. If a mod has no Fabric 1.21.11
target, keep that fact documented. This branch still exposes leftover `lsoCompat`,
`vsCollisionCompat`, `vsExplosionCompat`, and `mtsFix` keys as dormant options; do not describe
them as working integrations.
