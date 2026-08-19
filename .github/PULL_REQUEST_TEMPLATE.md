## Summary

-

## Validation

- [ ] `python3 scripts/download_dependencies.py --check-only`
- [ ] `python3 scripts/check_release_consistency.py`
- [ ] `python3 scripts/check_mod_icon.py`
- [ ] `python3 scripts/audit_port.py --strict` with the 1.21.11 TaCZ jar, named/intermediary Minecraft jars, and generated refmap
- [ ] `./gradlew clean build --stacktrace` on JDK 21
- [ ] Dedicated-server smoke test, if this affects common/server mixins or startup
- [ ] Optional-mod in-game test, if this changes compatibility behavior

## Checklist

- [ ] Tests added or updated where feasible
- [ ] Documentation updated for user-visible changes
- [ ] `CHANGELOG.md` updated
- [ ] Licenses/notices/manifest updated for any asset, library or vendored binary change
- [ ] Mixin target class/method descriptors and remap/refmap impact checked
- [ ] Release claims do not overstate validation actually performed
