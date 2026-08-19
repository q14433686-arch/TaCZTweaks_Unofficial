# Bullet interactions

Directory: `data/<namespace>/bullet_interactions/*.json`.

Rules control bullet behavior against blocks, entities and shields. The manager selects the
first matching rule for the relevant target type after sorting by `priority`. Use the
example pack files as the concrete 1.21.11 fixtures.

## Block rule

```json
{
  "type": "block",
  "blocks": [
    "#c:glass_blocks",
    "#c:glass_panes"
  ],
  "block_break": {
    "type": "dynamic_damage"
  },
  "pierce": {
    "type": "damage",
    "damage_falloff": 2
  }
}
```

Important fields from the 1.21.11 codec:

- `target`: selector or selector list. Optional.
- `blocks`: block IDs, tags, predicates or logical block tests accepted by `BlockTestable`.
- `block_break.type`: `never`, `instant`, `count`, `fixed_damage`, or `dynamic_damage`.
- Shared block-break options include `replace_with`, `hardness`, `tier`, and `drop` where the
  selected break type supports them. `count` requires `count`. `fixed_damage` requires
  `damage` and optional `accumulate`. `dynamic_damage` accepts `modifier`, `multiplier`, and
  `accumulate`.
- `pierce.type`: `never`, `default`, `count`, or `damage`. Options include `conditional`,
  `damage_falloff` / `damageFalloff`, `damage_multiplier` / `damageMultiplier`, and
  `render_bullet_hole`.
- `gun_pierce`: `{ "required": false, "consume": false }` whether TaCZ gun pierce capability
  is required/consumed.

Player-owned block breaking checks `Level.mayInteract` and the Fabric
`PlayerBlockBreakEvents` BEFORE/CANCELED/AFTER chain.

## Entity rule

The 1.21.11 entity codec uses a `damage` object, not a flat `damage_multiplier` field:

```json
{
  "type": "entity",
  "entities": ["minecraft:ender_dragon"],
  "damage": {
    "modifier": -10
  },
  "pierce": {
    "type": "never"
  }
}
```

`damage` accepts `modifier` and `multiplier`. Entity tests support IDs, tags, predicates and
logical composition. Entity rules also accept `pierce` and `gun_pierce`.

## Shield rule

Shield rules exist in the 1.21.11 codec and are applied through Minecraft
`BlocksAttacks`. The current example pack does not ship a shield fixture. The codec shape is:

```json
{
  "type": "shield",
  "priority": 0,
  "target": [{"type": "category", "values": ["shotgun"]}],
  "predicate": {},
  "damage": {
    "falloff": 0.0,
    "multiplier": 0.5
  },
  "disable": {
    "duration": 20,
    "chance": 1.0,
    "conditional": true
  },
  "durability": {
    "type": "fixed_damage",
    "damage": 4,
    "conditional": true
  }
}
```

`durability.type` is `fixed_damage` or `dynamic_damage`. Do not write the flatter 26.2-only
field names (`damage_multiplier`, `durability_damage`, `disable_ticks`) on this branch.

## Legacy migration

The old v2 bullet-interaction format is still decoded and converted during load; prefer
writing new files in the explicit typed format above. See [Migration](MIGRATION.md).
