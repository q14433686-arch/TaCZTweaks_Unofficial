# Contributing

Thank you for helping maintain this unofficial Fabric port. This repository is fixed to the
`arena/01a0193f-tacztweaks-unofficial` working branch in Arena sessions; normal GitHub work should
use feature branches and pull requests against the maintained release branch.

## Development environment

- JDK 21
- Python 3
- Gradle Wrapper from this repository
- The exact vendored jars declared in `RESOURCE_IMPORT_MANIFEST.tsv`

Useful commands:

```bash
python3 scripts/audit_port.py --strict
python3 scripts/check_mod_icon.py
./gradlew clean build --stacktrace
```

## Pull request expectations

- Do not add unlicensed assets, copied code, logs with private data, or mutable binary blobs.
- Update `LICENSES.md`, and `THIRD_PARTY_NOTICES.md` when changing
  vendored binaries, embedded libraries or redistributed resources.
- Update docs and `CHANGELOG.md` for user-visible behavior changes.
- Keep `fabric.mod.json`, `gradle.properties`, README dependency tables and publish docs aligned.
- For mixins, cite the target class/method descriptors and run the strict audit.
- Do not claim game, dedicated-server, or optional-mod validation unless you actually ran it.
- Include or update tests for pure logic, codecs, config serialization or release packaging when
  feasible.

## Optional compatibility evidence

New compatibility code should include the exact mod name, version, loader, Minecraft version,
source or binary inspected, test scenario, and known boundaries. If a mod has no Fabric 1.21.11
target, remove no-op switches rather than adding dormant configuration.