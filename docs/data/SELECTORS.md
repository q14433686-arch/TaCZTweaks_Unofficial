# Selectors

Selectors appear as `target` entries in bullet and melee data. A top-level target is normally a
list of selector objects; a rule matches when all listed selectors match unless a logical selector
changes that behavior.

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
| `ammo` | `values: [identifier...]` | Matches projectile ammo ID; bullet only. |
| `regex` | `match: "gun"|"ammo"`, `regex: string` | Java/Kotlin regex against the selected ID string. |
| `predicate` | `predicate: entity predicate` | Minecraft advancement entity predicate against the projectile entity. |
| `damage` | `values: [range...]` | Matches bullet damage. |
| `speed` | `values: [range...]` | Matches projectile speed as used by the port (`delta.length * 10`). |
| `silenced` | none | Matches a shooter/gun with TaCZ silence modifier cache. |
| `burst_index` | `index: int bounds` | Matches burst cycle index attached by the 26.1.2 shooting hook. |
| `pellet_index` | `index: int bounds` | Matches pellet index attached by the 26.1.2 projectile hook. |
| `random_chance` | `chance: double` | Random match probability. |

Ranges use Minecraft-style bounds, for example `{"min": 0.0, "max": 10.0}`. Keep regexes simple;
resource reload runs on content data and pathological patterns can hurt performance.

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
