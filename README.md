# [Tacz] NPCs

Minecraft Forge mod for **1.20.1** that adds armed NPC factions powered by [Timeless and Classics Zero (TACZ)](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero). Bandits and Duty units patrol the world, fight with TACZ guns, wear configurable gear sets, and can be customized through loot tables and item tags.

Fork maintained by [craftorio](https://github.com/craftorio/tacz-npcs). Original work by Corrinedev.

## Features

- **Bandits** — hostile NPCs that replace vanilla pillagers in world generation.
- **Duty** — neutral/guard-style NPCs; replace Guard Villagers guards when that mod is installed.
- **TACZ combat** — NPCs use the full TACZ gun system: aiming, reloading, attachments, and strafing.
- **Rank system** — Rookie, Experienced, Veteran, and Expert tiers affect stats and displayed name tags (via patch items).
- **Patrols** — groups spawn together with a leader carrying a patrol banner.
- **Lootable corpses** — right-click a dead NPC to open its inventory (configurable drop/despawn behavior).
- **Configurable damage** — separate multipliers for damage to players and to other entities.
- **Equipment tags** — armor and weapon sets are data-driven; add or remove sets without code changes.
- **Scav player** — experimental fake-player entity for advanced pathfinding (`/playerscav`).

## Requirements

| Mod | Notes |
|-----|-------|
| Minecraft **1.20.1** | |
| Forge **47.3.12+** | |
| [TACZ](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero) **1.0.3+** | Required |
| [Smart Brain Lib](https://www.curseforge.com/minecraft/mc-mods/smartbrainlib) | Required |

### Optional integrations

| Mod | Effect |
|-----|--------|
| [Guard Villagers](https://www.curseforge.com/minecraft/mc-mods/guard-villagers) | Guard entities are replaced with Duty NPCs |
| Gundurability | Gun durability values on spawned weapons use config range |

## Installation

1. Install Forge 1.20.1.
2. Place **TACZ**, **Smart Brain Lib**, and **tacz_npc** into the `mods` folder.
3. Start the game or server.

Pre-built JAR: `build/libs/tacz_npc-1.0.0-craftorio-all.jar` (after building).

## Configuration

Config files are created on first run:

- **Server:** `config/tacz_npc-common.toml`
- **Client:** `config/tacz_npc-client.toml`

| Option | Default | Description |
|--------|---------|-------------|
| `base multiplier` | `1.0` | NPC gun damage multiplier (non-players) |
| `base player multiplier` | `0.5` | NPC gun damage multiplier vs players |
| `dropitems` | `true` | NPCs drop items on death |
| `despawntime` | `2000` | Ticks before corpse despawns (~1.6 min) |
| `min` / `max` | `3` / `8` | Patrol size range |
| `from` / `to` | `200` / `800` | Gun durability range (with Gundurability) |
| `nametags` (client) | `true` | Show NPC name tags |

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/playerscav` | OP | Spawns a Scav player entity at your position (debug/pathfinding) |

## Customization

### Equipment sets (item tags)

Armor and weapon loadouts are defined in `data/tacz_npc/tags/items/`. Each tag file represents one full set for a faction and rank, for example:

- `bandit_grunt.json`, `bandit_thug.json`, `bandit_riot.json`, `bandit_officer.json`
- `duty_grunt.json`, `duty_thug.json`, `duty_riot.json`, `duty_officer.json`

Each set must include the matching **patch item** (`tacz_npc:rookie`, `experienced`, `veteran`, or `expert`) to assign rank. You can add new sets or change items without touching Java code.

If you add or rename sets, update the corresponding loot tables in `data/tacz_npc/loot_tables/` (`bandit.json`, `duty.json`).

See also: `data/tacz_npc/tags/items/readme-toedit.md`.

### World generation

- Pillager outposts and related structures spawn **Bandits** instead of pillagers (via datapack overrides).
- A **Duty checkpoint** jigsaw structure exists (`duty_checkpoint`), but its structure set placement is currently disabled in this fork.

## Building from source

```bash
./gradlew build
```

Output: `build/libs/tacz_npc-1.0.0-craftorio-all.jar`

Run a dev client:

```bash
./gradlew runClient
```

Requires Java **17**.

## License

MIT License — see [LICENSE](LICENSE).
