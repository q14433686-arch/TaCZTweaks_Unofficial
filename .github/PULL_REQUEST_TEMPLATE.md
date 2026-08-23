## Target release line

- [ ] `26.2(main)` Fabric 26.2
- [ ] `26.2-neoforge` NeoForge 26.2
- [ ] `26.1.2` Fabric 26.1.2
- [ ] `26.1.2-neoforge` NeoForge 26.1.2
- [ ] `1.21.11` Fabric 1.21.11
- [ ] `1.21.11-neoforge` NeoForge 1.21.11

Base this PR on the matching release branch. Do not mix loader or Minecraft lines.

## Summary

-

## Validation

- [ ] `python3 scripts/download_dependencies.py --check-only`
- [ ] `python3 scripts/audit_port.py --strict`
- [ ] `python3 scripts/check_mod_icon.py`
- [ ] `./gradlew clean build --stacktrace`
- [ ] Dedicated-server smoke test, if this affects common/server mixins or startup
- [ ] Optional-mod in-game test, if this changes compatibility behavior

## Checklist

- [ ] Tests added or updated where feasible
- [ ] Documentation updated for user-visible changes
- [ ] `CHANGELOG.md` updated
- [ ] Licenses/notices/manifest updated for any asset, library or vendored binary change
- [ ] Mixin target class/method descriptors checked
- [ ] Release claims do not overstate validation actually performed
