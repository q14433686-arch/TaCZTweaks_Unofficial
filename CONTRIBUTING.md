# Contributing

Thank you for helping maintain these unofficial ports. The repository has six release branches
(see [`docs/BRANCHES.md`](docs/BRANCHES.md)):

| Branch | Loader | Minecraft |
|---|---|---|
| `26.2(main)` (default) | Fabric | 26.2 |
| `26.2-neoforge` | NeoForge | 26.2 |
| `26.1.2` | Fabric | 26.1.2 |
| `26.1.2-neoforge` | NeoForge | 26.1.2 |
| `1.21.11` | Fabric | 1.21.11 |
| `1.21.11-neoforge` | NeoForge | 1.21.11 |

Open pull requests against the **matching** release branch. Do not land a NeoForge or 1.21.11
change only on `26.2(main)`. Arena session branches are temporary and are not release lines.

## Development environment

- JDK 25
- Python 3
- Gradle Wrapper from this repository
- The exact vendored jars declared in `RESOURCE_IMPORT_MANIFEST.tsv`

Useful commands:

```bash
python3 scripts/download_dependencies.py --check-only
python3 scripts/audit_port.py --strict
python3 scripts/check_mod_icon.py
./gradlew clean build --stacktrace
```

## Pull request expectations

- Do not add unlicensed assets, copied code, logs with private data, or mutable binary blobs.
- Update `RESOURCE_IMPORT_MANIFEST.tsv`, `LICENSES.md`, and `THIRD_PARTY_NOTICES.md` when changing
  vendored binaries, embedded libraries or redistributed resources.
- Update docs and `CHANGELOG.md` for user-visible behavior changes.
- Keep loader metadata (`fabric.mod.json` or NeoForge mods toml), `gradle.properties`, README
  dependency tables and publish docs aligned **on that branch**.
- For mixins, cite the target class/method descriptors and run the strict audit.
- Do not claim game, dedicated-server, or optional-mod validation unless you actually ran it.
- Include or update tests for pure logic, codecs, config serialization or release packaging when
  feasible.

## Optional compatibility evidence

New compatibility code should include the exact mod name, version, loader, Minecraft version,
source or binary inspected, test scenario, and known boundaries. If a mod has no Fabric 26.2 target,
remove no-op switches rather than adding dormant configuration.
