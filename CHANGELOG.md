## 3.0.0-alpha.10.unofficial.2 — Minecraft 1.21.11 Fabric

- Audited all 163 configured Mixins and replaced remaining 1.21.11 lambda, internal-call, shared-local, and arbitrary-local dependencies with stable method boundaries where possible.
- Fixed per-line Stonecutter conversion for `Identifier` parameters in newly multiline Java declarations and audited all 178 immediate-line replacement directives.
- Added a committed port audit at `docs/PORT_1_21_11_AUDIT.md` with evidence and release gates.
- Added a Stonecutter target for Minecraft 1.21.11 and Java 21.
- Targets `[UNOFFICIAL] TaCZ Refabricated` R2 through CurseForge project `1627909`, file `8660664`.
- Updated Fabric Loader, Fabric API, YACL, Mod Menu, Cloth Config and Forge Config API Port dependencies.
- Migrated Mojang's `ResourceLocation` name to `Identifier`, key categories to the new registered category API, tool tiers to `ToolMaterial`, and JSON reloaders to the modern preparable API.
- Re-anchored shooting, reload, bolt, animation and attachment Mixins to TaCZ R2's stable named hooks instead of compiler-generated lambda names.
- Added support for the TaCZ fork's built-in LRTactical implementation.
- Ported crawl rendering to `AvatarRenderer` and shield interactions to the `BlocksAttacks` component pipeline.
- Kept the existing 1.20.1 and 1.21.1 source targets through Stonecutter conditionals.

## TaCZ Tweaks V3 Alpha
Status: Feature parity with V2; feature incomplete for V3  
It's marked as alpha because it's still feature incomplete, not because it's unstable per se  
The config file will be migrated automatically; this version should serve as a drop-in replacement

### Summary of Changes
- New logo
- Complete rewrite
- Multi-loader, multi-version support (`1.20.1-forge`, `1.21.1-neoforge`, `1.20.1-fabric` and `1.21.1-fabric`)
- Reorganized config screen and config structure
- New attributes system (modify damage, disable guns, etc.)
- New status effect (disarm)
- New command (`/tacztweaks refill_ammo [<targets>]`)
- New options (Sable/Aeronautics compat, Cuffed compat, bolt key, etc.)

### Planned Features
- New overhauled and much more capable data pack system
- More attributes (suggestions welcome!)
- More commands (suggestions welcome!)

### Changes Over 3.0.0-alpha.9
- Implemented additional Sable compat for the LesRaisins Tactical Equipment mod
- Fixed compatibility with older versions of Sound Physics Remastered
- Fixed crashes related to attributes