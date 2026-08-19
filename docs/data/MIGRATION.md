# Data migration notes

## Bullet interaction v2 files

The port still accepts the older v2 bullet-interaction shape used by previous TaCZ Tweaks data, for
example flat `blocks`, `guns`, `block_break`, `pierce`, and `drop` fields. During reload those files
are converted to the typed `type: "block"` model.

New packs should use explicit typed rules:

```json
{
  "type": "block",
  "priority": 0,
  "target": [{"type": "gun", "values": ["tacz:ak47"]}],
  "blocks": ["minecraft:glass"],
  "block_break": {"type": "count", "count": 2, "drop": true},
  "pierce": {"type": "count", "count": 2, "damage_falloff": 1.0}
}
```

## Validation workflow

1. Put the pack in the normal Minecraft resource/data pack location.
2. Run `/reload`.
3. Watch logs for TaCZ Tweaks data manager messages and codec errors.
4. Reduce failing packs to a single JSON file before filing an issue.
5. Attach the minimal pack and full log.

## Removed config keys

Data packs do not restore removed legacy config options. Remove `thirdPersonGunRenderingFix`,
`lsoCompat`, `mtsFix`, `vsCollisionCompat`, and `vsExplosionCompat` from old configs.
