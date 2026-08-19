# Bullet interactions

Directory: `data/<namespace>/bullet_interactions/*.json`.

Rules control bullet behavior against blocks, entities and shields. The manager selects the first
matching rule for the relevant target type after sorting by `priority`; use lower or higher values
consistently within a pack and test conflicts with `/reload`.

## Block rule

```json
{
  "type": "block",
  "priority": 0,
  "target": [{"type": "gun", "values": ["tacz:ak47"]}],
  "blocks": ["minecraft:glass"],
  "block_break": {"type": "count", "count": 2, "drop": true},
  "pierce": {"type": "count", "count": 1, "damage_falloff": 1.0},
  "gun_pierce": {"required": false, "consume": false}
}
```

Important fields:

- `blocks`: block IDs, tags, predicates or logical block tests accepted by the codec.
- `block_break.type`: `never`, `instant`, `count`, `fixed_damage`, or `dynamic_damage`.
- Shared block-break options include `replace_with`, `hardness`, `tier`, and `drop` where the
  selected break type supports them.
- `pierce.type`: `never`, `default`, `count`, or `damage`; options include `conditional`,
  `damage_falloff`, `damage_multiplier`, and `render_bullet_hole`.
- `gun_pierce`: whether TaCZ gun pierce capability is required/consumed.

Player-owned block breaking checks `Level.mayInteract` and the Fabric
`PlayerBlockBreakEvents` BEFORE/CANCELED/AFTER chain.

## Entity rule

```json
{
  "type": "entity",
  "priority": 0,
  "target": [{"type": "ammo", "values": ["tacz:50bmg"]}],
  "entities": ["minecraft:ender_dragon"],
  "damage_multiplier": 2.0
}
```

Entity tests support IDs, tags, predicates and logical composition. Use selectors for gun/ammo,
damage, speed, burst, pellet and random conditions.

## Shield rule

```json
{
  "type": "shield",
  "priority": 0,
  "target": [{"type": "category", "values": ["shotgun"]}],
  "damage_multiplier": 0.5,
  "durability_damage": 4,
  "disable_ticks": 20
}
```

Shield behavior is implemented against Minecraft 26.1.2 `BlocksAttacks` components, not the old
Forge shield event.

## Legacy migration

The old v2 bullet-interaction format is still decoded and converted during load; prefer writing
new files in the explicit typed format above. See [Migration](MIGRATION.md).
