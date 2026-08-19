# Data-driven content formats

This page describes the Fabric **1.21.11** codecs. TaCZ Tweaks loads JSON from
`data/<namespace>/<directory>/*.json` through Minecraft resource reload. File IDs are sorted
by `priority` where the relevant manager selects the first matching rule or all matching
rules depending on system.

- [Selectors](SELECTORS.md)
- [Bullet interactions](BULLET_INTERACTIONS.md)
- [Bullet sounds](BULLET_SOUNDS.md)
- [Bullet particles](BULLET_PARTICLES.md)
- [Melee interactions](MELEE_INTERACTIONS.md)
- [Migration notes](MIGRATION.md)

The example pack in `tacz-tweaks-example-pack/` is the canonical runnable fixture for
bullet interactions, sounds and particles. It currently has no melee example file. If docs
and code disagree, treat the Kotlin codecs under
`src/main/kotlin/me/muksc/tacztweaks/data/` as the source of truth and file an issue.
