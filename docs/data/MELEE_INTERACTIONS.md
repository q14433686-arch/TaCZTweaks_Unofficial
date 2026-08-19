# Melee interactions

Directory: `data/<namespace>/melee_interactions/*.json`.

Melee interactions apply to gun butt/bayonet attacks and the target-side LRTactical
`IMeleeWeapon#performAttack` path. The 1.21.11 codec currently defines only `type: "block"`
rules. The example pack does not yet ship a melee fixture.

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

`block_break` reuses the bullet-interaction block-break codec (`never`, `instant`, `count`,
`fixed_damage`, `dynamic_damage`).

Selectors are evaluated without a bullet entity for pure melee, so selectors that require a
bullet (`ammo`, bullet speed, burst/pellet indices) will not match unless the relevant
runtime context is available.

## Permissions and safety

Block interactions should be validated on servers with protection/claim mods. The bullet
block path calls the Fabric block-break event chain; maintain the same discipline for melee
packs and never claim compatibility with a protection mod until tested.
