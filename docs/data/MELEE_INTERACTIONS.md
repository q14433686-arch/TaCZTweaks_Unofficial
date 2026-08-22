# Melee interactions

Directory: `data/<namespace>/melee_interactions/*.json`.

Melee interactions apply to gun butt/bayonet attacks and the target-side LRTactical
`IMeleeWeapon#performAttack` path. They are useful for controlled block breaking or special melee
behavior without hardcoding gun IDs in the mod.

## Example

```json
{
  "type": "block",
  "priority": 0,
  "target": [{"type": "gun", "values": ["tacz:ak47"]}],
  "blocks": ["minecraft:glass"],
  "block_break": {"type": "instant", "drop": false}
}
```

Selectors are evaluated without a bullet entity for pure melee, so selectors that require a bullet
(`ammo`, bullet speed, burst/pellet indices) will not match unless the relevant runtime context is
available.

## Permissions and safety

Block interactions should be validated on servers with protection/claim mods. The NeoForge bullet
and melee paths use `Level#mayInteract` plus `BreakBlockEvent`, not Fabric's three-stage chain;
never claim compatibility with a protection mod until tested.
