# CurseForge project copy

## Project name

TaCZ Tweaks (Renovated)

## Summary

Unofficial NeoForge TaCZ Tweaks port with configurable gunplay, movement, balancing, sounds,
interactions and data-driven systems for TaCZ: Renovated.

## Description

**This is an unofficial community port of TaCZ Tweaks by MUKSC.** It is not affiliated with or
endorsed by MUKSC, the TACZ Dev Team, TaCZ: Renovated maintainers, or optional-mod authors. Problems
introduced by this port should be reported to this project's issue tracker.

The port provides:

- configurable reload, unload, sprint, fire-mode, aiming, crawl and tilt behavior;
- damage, headshot, armor-ignore, projectile, spread, RPM and recoil modifiers;
- data-driven bullet block/entity/shield interactions;
- conditional sounds, airspace integration, particles and melee interactions;
- a YACL configuration screen through NeoForge's mod list;
- a documented reloadable example pack.

### Installation

Install the exact Minecraft/NeoForge build and dependency versions shown on each uploaded file.
TaCZ: Renovated and YetAnotherConfigLib are required; YACL is required on both client and server for
shared config persistence/sync. Kotlin stdlib is nested in the TaCZ Tweaks jar.

Sound Physics Remastered, First Aid New and Pillager’s Gun integrations are optional and constrained
by file metadata. Do not interpret an optional relation as an unqualified gameplay-test claim.

### Block-protection boundary

This loader lacks Fabric's BEFORE/CANCELED/AFTER block-break chain. Data-driven bullet/melee breaking
uses the loader's cancellable break-attempt event and `mayInteract`; protection-mod compatibility
must be tested per mod.

### Support requirements

Reports need exact versions, full `latest.log`, crash report where applicable, minimal reproduction,
all content packs/optional mods, and the result with TaCZ Tweaks removed. Do not send port-specific
reports to upstream projects.

### License

GPL-3.0. Original TaCZ Tweaks by MUKSC. See the repository for source, notices and dependency
checksums.

## File upload checklist

- Game version: exactly the value declared by the file.
- Mod loader: NeoForge.
- Required relations: TaCZ: Renovated and YetAnotherConfigLib.
- Optional relations: only integrations inside the file's declared ranges.
- Release type and validation wording: match `CHANGELOG.md`.
- Upload only the release jar; publish the example pack as a clearly separate artifact if desired.
