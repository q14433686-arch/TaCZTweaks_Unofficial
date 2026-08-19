# Selectors

Selectors appear as `target` entries in bullet and melee data. A top-level target may be a
single selector object or a list; a rule matches when all listed selectors match unless a
logical selector changes that behavior.

## Common shape

```json
{
  "type": "gun",
  "values": ["tacz:ak47"]
}
```

## Selector types

| Type | Fields | Notes |
|---|---|---|
| `all_of` | `terms: [selector...]` | All nested selectors must match. |
| `any_of` | `terms: [selector...]` | At least one nested selector must match. |
| `inverted` | `term: selector` | Negates the nested selector. |
| `gun` | `values: [identifier...]` | Matches the TaCZ gun ID. |
| `category` | `values: [string...]` | Matches TaCZ gun type/category lowercased with US locale. |
| `ammo` | `values: [identifier...]` | Matches projectile ammo ID; requires a bullet entity. |
| `regex` | `match: "gun"\|"ammo"`, `regex: string` | Java/Kotlin regex against the selected ID string. |
| `predicate` | `predicate: entity predicate` | Minecraft advancement entity predicate against the projectile entity. |
| `damage` | `values: [range...]` | Matches bullet damage. |
| `speed` | `values: [range...]` | Matches projectile speed as used by this port (`delta.length * 10`). |
| `silenced` | none | Matches a shooter/gun with TaCZ silence modifier cache. |
| `burst_index` | `index: int bounds` | Matches burst cycle index attached by the 1.21.11 shooting hook. |
| `pellet_index` | `index: int bounds` | Matches pellet index attached by the 1.21.11 projectile hook. |
| `random_chance` | `chance: float 0..1` | Random match probability. |

Ranges use Minecraft-style bounds, for example `{"min": 0.0, "max": 10.0}`. Keep regexes
simple; resource reload runs on content data and pathological patterns can hurt performance.

The example pack's `bullet_interactions/schema_smoke.json` exercises `all_of`, `gun`,
`predicate`, `burst_index` and `pellet_index` together.

## Example

```json
{
  "type": "all_of",
  "terms": [
    {"type": "category", "values": ["sniper"]},
    {"type": "damage", "values": [{"min": 8.0}]},
    {"type": "inverted", "term": {"type": "silenced"}}
  ]
}
```
