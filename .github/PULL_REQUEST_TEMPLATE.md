## Summary

-

## Validation

- [ ] `python3 scripts/download_dependencies.py --check-only`
- [ ] `python3 scripts/audit_port.py --strict`
- [ ] `python3 scripts/check_mod_icon.py`
- [ ] `python3 scripts/check_release_consistency.py`
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
