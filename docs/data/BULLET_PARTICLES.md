# Bullet particles

Directory: `data/<namespace>/bullet_particles/*.json`.

Bullet particle rules spawn particles for block or entity impacts. Particles are sent only to
the dimension where the event occurs.

The 1.21.11 codec uses `hit` / `pierce` / `break` or `kill` particle objects, not a flat
`particles` array with `offset`.

## Block example

```json
{
  "type": "block",
  "hit": {
    "particle": "minecraft:block{block_state:\"%s\"}"
  }
}
```

## Entity example

```json
{
  "type": "entity",
  "hit": {
    "particle": "minecraft:block{block_state:\"minecraft:redstone_block\"}"
  },
  "pierce": {
    "particle": "minecraft:block{block_state:\"minecraft:redstone_block\"}"
  },
  "kill": {
    "particle": "minecraft:block{block_state:\"minecraft:redstone_block\"}",
    "delta": {"type": "absolute", "x": 0.2, "y": 0.2, "z": 0.2},
    "count": 20
  }
}
```

Each particle object accepts:

- `particle` (required string; may include a format token such as `%s` for the hit block)
- `position` and `delta` coordinates: `absolute`, `relative`, or `local`, each with `x`/`y`/`z`
- `speed`, `count`, `force`, `duration`
- optional nested `target` and extra `blocks` / `entities` filters

Default position is relative `(0,0,0)`; default delta is absolute `(0,0,0)`.

## Notes

- Use selectors to narrow particle rules by gun, ammo, speed, damage, burst or pellet.
- Prefer tags for block/entity groups.
- Malformed particle JSON is rejected during resource reload and should be debugged with the
  matching reload log attached to issues.
