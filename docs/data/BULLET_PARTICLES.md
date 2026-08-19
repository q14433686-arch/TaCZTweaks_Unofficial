# Bullet particles

Directory: `data/<namespace>/bullet_particles/*.json`.

Bullet particle rules spawn particles for block or entity impacts. Particles are sent only to the
dimension where the event occurs.

## Example

```json
{
  "type": "block",
  "priority": 0,
  "target": [{"type": "category", "values": ["rifle"]}],
  "blocks": ["minecraft:stone"],
  "particles": [
    {
      "particle": "minecraft:poof",
      "count": 8,
      "offset": [0.1, 0.1, 0.1],
      "speed": 0.01
    }
  ]
}
```

Coordinate handling supports absolute, relative and local vectors in the codecs. Use the example
pack's `bullet_particles` files as concrete fixtures and keep high-count or continuous emitters
small enough for multiplayer servers.

## Notes

- Use selectors to narrow particle rules by gun, ammo, speed, damage, burst or pellet.
- Prefer tags for block/entity groups.
- Malformed particle JSON is rejected during resource reload and should be debugged with the
  matching reload log attached to issues.
