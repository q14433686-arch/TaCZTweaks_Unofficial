# Contributing

Thank you for helping maintain this unofficial NeoForge 26.2 port. Use a feature branch and pull
request against the maintained NeoForge release line; do not mix Fabric loader APIs into this tree.
Gameplay semantics should be compared with `26.2(main)`, while loader patterns should be compared
with the proven 26.1.2 NeoForge port and TaCZ: Renovated 26.2.

## Development environment

- JDK 25
- Python 3 for standalone audits
- this repository's Gradle Wrapper
- exact local jars declared in `RESOURCE_IMPORT_MANIFEST.tsv`

```bash
python3 scripts/download_dependencies.py --check-only
python3 scripts/check_mod_icon.py
python3 scripts/check_release_consistency.py
python3 scripts/audit_port.py --strict
./gradlew test
./gradlew clean build --stacktrace
```

A strict audit without TaCZ/Minecraft jars is incomplete even if all Python-only checks pass.

## Pull request expectations

- Do not add unlicensed assets, private logs, credentials, or mutable binary blobs.
- Keep `gradle.properties`, `neoforge.mods.toml`, README, BUILD, CHANGELOG and dependency docs aligned.
- Update `RESOURCE_IMPORT_MANIFEST.tsv`, `LICENSES.md` and `THIRD_PARTY_NOTICES.md` when dependencies
  or redistributed resources change.
- Cite non-trivial APIs as class + method signature + source in `docs/records/`.
- For every mixin, verify target side, target descriptor, `@At` owner/name/descriptor, and relevant
  NeoForge 26.2.x patches.
- Do not call source checks, disabled paths or a successful compile “game tested.”
- Common/server changes require dedicated-server startup and `scripts/check_server_log.py`.
- Client release validation requires main-menu startup and the documented gameplay smoke matrix.

## Optional compatibility evidence

Record the exact mod version, loader, Minecraft version, source or jar inspected, test scenario and
known boundary. If no verified NeoForge 26.2 target exists, say “unsupported/not verified” and do not
add a dormant configuration switch.
