# Contributing

Thank you for helping maintain this unofficial Fabric 1.21.11 port. Arena sessions on this
checkout are fixed to `arena/01a019b0-tacztweaks-unofficial`. Normal GitHub work should use
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
python3 scripts/check_release_consistency.py
python3 scripts/check_mod_icon.py
python3 scripts/audit_port.py --strict \
  --tacz-jar libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar \
  --minecraft-named-jar <1.21.11 named jar> \
  --minecraft-intermediary-jar <1.21.11 intermediary jar> \
  --refmap build/resources/main/tacztweaks.refmap.json
./gradlew clean build --stacktrace
```

## Pull request expectations

- Do not add unlicensed assets, copied code, logs with private data, or mutable binary blobs.
- Update `RESOURCE_IMPORT_MANIFEST.tsv`, `LICENSES.md`, and `THIRD_PARTY_NOTICES.md` when changing
  vendored binaries, embedded libraries or redistributed resources.
- Update docs and `CHANGELOG.md` for user-visible behavior changes.
- Keep `fabric.mod.json`, `gradle.properties`, README dependency tables and publish docs aligned.
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
