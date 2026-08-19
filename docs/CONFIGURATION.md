# Configuration reference

Config file: `config/tacztweaks.json`.

This page describes the Fabric **1.21.11** branch. The YACL/Mod Menu screen is the preferred
editor. Options marked **synced** are sent by the server to joining clients and require
permission to change while connected to that server. Client-only options are local
UI/audio/rendering preferences. Most values apply immediately after saving; data-pack driven
systems still reload through Minecraft resource reload.

Some keys exist in the JSON schema even when they are not shown on every YACL screen.

## Gun

| Key | Default | Scope | Effect |
|---|---:|---|---|
| `gun.shootWhileSprinting` | `true` | synced | Allows shooting while sprinting. |
| `gun.sprintWhileReloading` | `true` | synced | Allows sprinting during reload. |
| `gun.reloadWhileShooting` | `true` | synced | Allows reload input while the weapon is shooting. |
| `gun.reloadDiscardsMagazine` | `false` | synced | Discards remaining magazine ammunition during reload, except listed guns. |
| `gun.reloadDiscardsMagazineExclusions` | `['tacz:m870','tacz:db_short','tacz:db_long']` | synced | Gun IDs exempt from magazine discard. |
| `gun.fireSelectWhileShooting` | `false` | synced | Allows fire-mode switching during shooting. |
| `gun.disableUnderwater` | `false` | synced | Prevents gun firing underwater. |
| `gun.manualBolting` | `false` | client/local | Requires explicit bolt input where supported. |
| `gun.allowUnload` | `true` | synced | Enables unload key behavior. |
| `gun.unloadBulletInBarrel` | `false` | synced | Also unloads chambered ammunition when unloading. |
| `gun.reduceSensitivityKeyMultiplier` | `0.5` | client/local | Multiplier while the reduce-sensitivity key is active. |
| `gun.disableReduceSensitivityKeyWhileAiming` | `false` | client/local | Ignores reduce-sensitivity key while aiming. |
| `gun.tiltGunKeyCancelsSprint` | `true` | client/local | Cancels sprint when using the tilt-gun key. |
| `gun.tiltGunKeyTriggersReduceSensitivity` | `true` | client/local | Tilt key also triggers reduced sensitivity. |
| `gun.cancelInspection` | `false` | client/local | Allows repeated inspect input to cancel inspection. |
| `gun.disableBulletCulling` | `false` | client/local | Disables projectile render culling. |

## Modifiers

Each modifier entry has the shape below and is **synced**:

```json
{
  "addend": 0.0,
  "multiplier": 1.0,
  "function": ""
}
```

Empty modifiers leave the TaCZ baseline unchanged. Available modifier keys are:

`damage`, `playerDamage`, `headshot`, `playerHeadshot`, `armorIgnore`, `speed`, `gravity`,
`friction`, `aimTime`, `inaccuracy`, `standInaccuracy`, `aimInaccuracy`, `moveInaccuracy`,
`sneakInaccuracy`, `crawlInaccuracy`, `rpm`, `verticalRecoil`, `horizontalRecoil`,
`aimVerticalRecoil`, `aimHorizontalRecoil`, `crawlVerticalRecoil`, and
`crawlHorizontalRecoil`.

## Crawl

| Key | Default | Scope | Effect |
|---|---:|---|---|
| `crawl.enabled` | `true` | synced | Enables crawl-related gameplay integration. |
| `crawl.pitchUpperLimit` | `25.0` | client/local | Upper pitch bound while crawling. |
| `crawl.pitchLowerLimit` | `-10.0` | client/local | Lower pitch bound while crawling. |
| `crawl.dynamicPitchLimit` | `false` | client/local | Dynamically adjusts crawl pitch limits. |
| `crawl.visualTweak` | `true` | client/local | Smooths 1.21.11 `AvatarRenderer` crawl transition rendering. |
| `crawl.tiltGun` | `DEFAULT` | client/local | Gun tilt policy: `DEFAULT`, `ALWAYS`, or `NEVER`. |

## Compatibility

| Key | Default | Scope | Effect |
|---|---:|---|---|
| `compat.firstAidCompat` | `true` | synced | Enables First Aid New hit-location compatibility when that 1.21.11 legacy mod is loaded. |
| `compat.lsoCompat` | `true` | synced | **Dormant.** No verified Fabric 1.21.11 Legendary Survival Overhaul target is wired. |
| `compat.vsCollisionCompat` | `false` | synced | **Dormant.** No verified Fabric 1.21.11 Valkyrien Skies target is wired. |
| `compat.vsExplosionCompat` | `false` | synced | **Dormant.** No verified Fabric 1.21.11 Valkyrien Skies target is wired. |
| `compat.mtsFix` | `true` | synced | **Dormant.** No verified Fabric 1.21.11 Immersive Vehicles target is wired. |

Sound Physics Remastered and Pillager's Gun integration is auto-detected when those optional
mods are present in the declared version ranges. The dormant keys remain in the JSON schema so
old configs still load; they do not enable a 1.21.11 integration.

## Tweaks

| Key | Default | Scope | Effect |
|---|---:|---|---|
| `tweaks.audibleFirstPersonGunSounds` | `false` | synced | Makes first-person gun sounds audible to the shooter where TaCZ would suppress them. |
| `tweaks.forceFirstPersonShootingSound` | `false` | synced | Forces first-person shooting sound playback path. |
| `tweaks.betterMonoConversion` | `false` | client/local | Uses per-request mono conversion cache for TaCZ sounds. Present in JSON; confirm the current YACL screen if you need a GUI toggle. |
| `tweaks.betterInaccuracy` | `false` | synced | Enables improved inaccuracy handling. |
| `tweaks.betterGunTilt` | `false` | synced | Enables improved gun tilt behavior. |
| `tweaks.bulletProtection` | `false` | synced | Restores projectile-protection contribution for TaCZ bullet damage, excluding void bullets. |
| `tweaks.endermenEvadeBullets` | `false` | synced | Lets endermen evade configured bullet hits. |
| `tweaks.disableRefitOnAdventure` | `false` | synced | Disables gun refit on adventure-mode players. |
| `tweaks.infiniteAmmoDisablesConsumption` | `false` | synced | Endless-ammo status prevents ammo consumption. |
| `tweaks.alwaysFilterByHand` | `true` | client/local | Workbench/gun-pack list filters by held item. |
| `tweaks.rps` | `false` | client/local | Shows rounds-per-second related UI. |
| `tweaks.forceDefaultHitAndKillSounds` | `false` | client/local | Forces default hit/kill sound behavior. |
| `tweaks.suppressHeadHitSounds` | `false` | client/local | Suppresses head-hit sound. |
| `tweaks.suppressFleshHitSounds` | `false` | client/local | Suppresses flesh-hit sound. |
| `tweaks.suppressKillSounds` | `false` | client/local | Suppresses kill sound. |
| `tweaks.hideHitMarkers` | `false` | client/local | Hides hit marker rendering. |

## Debug

All debug options default to `false` and are **synced**: `debug.bulletInteractions`,
`debug.bulletParticles`, `debug.bulletSounds`, and `debug.meleeInteractions`. Enable them only
for diagnosis because they may increase log volume.

## Removed or unused legacy options

- `thirdPersonGunRenderingFix` is **not present**. TaCZ Refabricated 1.21.11 R2 already fixes
  the target behavior in `ItemInHandLayerMixin`.
- `lsoCompat`, `mtsFix`, `vsCollisionCompat`, and `vsExplosionCompat` still exist as keys but
  have no 1.21.11 implementation. Remove them from old configs if they cause confusion.
