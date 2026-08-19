# Bullet sounds

Directory: `data/<namespace>/bullet_sounds/*.json`.

Bullet sound rules can play constant sounds, block-hit sounds, entity-hit sounds, whizz sounds and
Sound Physics airspace-conditioned sounds. Sound identifiers must also be present in the resource
pack's `assets/<namespace>/sounds.json` when custom audio is used.

## Constant sound

```json
{
  "type": "constant",
  "priority": 0,
  "target": [{"type": "gun", "values": ["tacz:ak47"]}],
  "sound": "minecraft:entity.arrow.shoot",
  "volume": 0.5,
  "pitch": 1.0,
  "range": 16.0
}
```

## Block/entity hit sounds

Block and entity rules add `blocks` or `entities` tests to the common sound fields:

```json
{
  "type": "block",
  "priority": 0,
  "target": [{"type": "category", "values": ["rifle"]}],
  "blocks": ["#tacztweaks:metal"],
  "sound": "tacztweaks:hit.metal",
  "volume": 1.0,
  "pitch": 1.0,
  "range": 24.0
}
```

## Whizz sounds

Whizz sounds are evaluated near players as bullets pass. Keep ranges conservative to avoid noisy
packs and unnecessary network/audio load.

## Airspace sounds

Airspace rules require Sound Physics Remastered at runtime. They select by `airspace`, `occlusion`
and `reflectivity` ranges and then play thresholded sounds, for example see
`tacz-tweaks-example-pack/data/tacztweaks/bullet_sounds/airspace.json`.

```json
{
  "type": "airspace",
  "priority": -1000,
  "target": [{"type": "gun", "values": ["tacz:example"]}],
  "airspace": {"min": 0.0, "max": 1.0},
  "occlusion": {"min": 0.0, "max": 1.0},
  "reflectivity": {"min": 0.0, "max": 1.0},
  "sounds": [
    {"threshold": 16.0, "sound": {"sound": "minecraft:entity.arrow.shoot", "volume": 0.5, "pitch": 1.0, "range": 16.0}}
  ]
}
```
