# Bullet sounds

Directory: `data/<namespace>/bullet_sounds/*.json`.

Bullet sound rules can play constant sounds, block-hit sounds, entity-hit sounds, whizz
sounds and Sound Physics airspace-conditioned sounds. Sound identifiers must also be present
in the resource pack's `assets/<namespace>/sounds.json` when custom audio is used.

The 1.21.11 block/entity codecs use `hit` / `pierce` / `break` or `kill` lists, not a single
top-level `sound` field.

## Constant sound

`interval` is required and must be positive:

```json
{
  "type": "constant",
  "target": {
    "type": "ammo",
    "values": ["tacz:rpg_rocket"]
  },
  "interval": 2,
  "sounds": [
    {"sound": "minecraft:item.firecharge.use"}
  ]
}
```

Each sound object accepts `sound`, optional `volume`, `pitch`, `range`, and a nested
`target`.

## Block/entity hit sounds

```json
{
  "type": "block",
  "blocks": ["#tacztweaks:metal"],
  "hit": {
    "sound": "tacztweaks:generic.hit.metal"
  }
}
```

Block rules also accept `pierce` and `break` sound lists. Entity rules accept `hit`,
`pierce` and `kill`. Nested sound objects may further filter by `blocks` or `entities`.

## Whizz sounds

Whizz sounds are evaluated near players as bullets pass. Thresholds are sorted by the codec.
Keep ranges conservative to avoid noisy packs and unnecessary network/audio load.

```json
{
  "type": "whizz",
  "target": {
    "type": "speed",
    "values": [{"min": 10.0}]
  },
  "sounds": [
    {"threshold": 2.0, "sound": {"sound": "tacztweaks:generic.whizz.near"}},
    {"threshold": 5.0, "sound": {"sound": "tacztweaks:generic.whizz.mid"}},
    {"threshold": 10.0, "sound": {"sound": "tacztweaks:generic.whizz.far"}}
  ]
}
```

## Airspace sounds

Airspace rules require Sound Physics Remastered at runtime. They select by `airspace`,
`occlusion` and `reflectivity` ranges. See
`tacz-tweaks-example-pack/data/tacztweaks/bullet_sounds/airspace.json`.

```json
{
  "type": "airspace",
  "priority": -1000,
  "target": {
    "type": "gun",
    "values": ["tacztweaks:codec_smoke"]
  },
  "airspace": {"min": 0.0, "max": 1.0},
  "occlusion": {"min": 0.0, "max": 1.0},
  "reflectivity": {"min": 0.0, "max": 1.0},
  "sounds": [
    {
      "threshold": 16.0,
      "sound": {
        "sound": "minecraft:entity.arrow.shoot",
        "volume": 0.5,
        "pitch": 1.0,
        "range": 16.0
      }
    }
  ]
}
```
