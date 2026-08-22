# Modrinth project copy

## Name

TaCZ Tweaks (Renovated)

## Summary

An unofficial NeoForge port adding configurable gunplay, movement, balance, interactions, sound,
and data-driven customization for TaCZ: Renovated.

## Description

> **Unofficial community port.** This project is not affiliated with or endorsed by MUKSC, the
> TACZ Dev Team, TaCZ: Renovated maintainers, or optional compatibility-mod authors. Report issues
> caused by this port here, not to those projects.

TaCZ Tweaks (Renovated) carries MUKSC's configurable gameplay and data-driven systems to the
NeoForge TaCZ: Renovated line. Features include gun movement/reload/unload controls, global
modifiers, crawl and aiming behavior, bullet interactions, conditional sounds and particles, melee
interactions, and a YACL configuration screen opened from NeoForge's mod list.

### Required dependencies

Install the exact Minecraft/NeoForge, TaCZ: Renovated and YetAnotherConfigLib versions declared by
the uploaded file. YACL is required on both physical sides because shared config persistence and
sync use its config types. Kotlin stdlib is embedded by this mod.

### Optional integrations

File metadata defines the supported ranges for Sound Physics Remastered, First Aid New and
Pillager’s Gun (Unofficial Port). LRTactical is supplied by TaCZ: Renovated. A listed optional range
means a guarded integration exists; release notes must separately say whether it was tested in game.

### Data packs

The repository includes a reloadable example pack and schema documentation for bullet interactions,
bullet sounds, bullet particles, melee interactions and selectors.

### Known boundary

NeoForge does not provide Fabric's three-stage player block-break callback chain. Bullet/melee block
rules use `mayInteract` plus a cancellable break-attempt event; protection-mod behavior is not claimed
without explicit testing.

### Support

When reporting a bug, include exact versions, complete logs, minimal reproduction steps and the
result after removing TaCZ Tweaks. Do not redistribute third-party content without permission.

### License and source

GPL-3.0. Original mod by MUKSC. Source, issue tracker, dependency provenance and third-party notices
are linked from the repository.

## Upload checklist

- Select **NeoForge** as loader and the exact Minecraft version supported by the file.
- Add TaCZ: Renovated and YetAnotherConfigLib as required dependencies.
- Add only optional relations whose ranges match file metadata.
- Copy validation status from the release changelog; do not infer it from older files.
- Attach the release jar, not local dependencies or the example-pack zip in place of the mod jar.
