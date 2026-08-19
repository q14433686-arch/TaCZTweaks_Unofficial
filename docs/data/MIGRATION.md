# Data migration notes

## Bullet interaction v2 files

The 1.21.11 port still accepts the older v2 bullet-interaction shape used by previous TaCZ
Tweaks data, for example flat `blocks`, `guns`, `block_break`, `pierce`, and `drop` fields.
During reload those files are converted to the typed `type: "block"` model. The fixture
`src/test/resources/fixtures/bullet_interaction_v2.json` is the checked example.

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

## Branch-specific field names

Do not copy 26.2 documentation examples blindly:

- entity damage is `{ "modifier": ..., "multiplier": ... }`, not `damage_multiplier`;
- shield rules use `damage.falloff` / `damage.multiplier`, `disable.duration`, and a typed
  `durability` object;
- sounds and particles use `hit` / `pierce` / `break` / `kill` objects;
- constant sounds require a positive `interval`.

## Validation workflow

1. Put the pack in the normal Minecraft resource/data pack location.
2. Run `/reload`.
3. Watch logs for TaCZ Tweaks data manager messages and codec errors.
4. Reduce failing packs to a single JSON file before filing an issue.
5. Attach the minimal pack and full log.

## Removed or dormant config keys

Data packs do not restore removed legacy config options. `thirdPersonGunRenderingFix` is
absent. `lsoCompat`, `mtsFix`, `vsCollisionCompat`, and `vsExplosionCompat` may still appear
in old configs but have no 1.21.11 implementation.
