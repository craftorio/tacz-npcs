# Agent guide — tacz-npcs

Instructions for AI coding agents working in this repository.

## Project

Minecraft **Forge 1.20.1** mod (`mod_id`: `tacz_npc`) that adds armed NPC factions integrated with **Timeless and Classics Zero (TACZ)** and **Smart Brain Lib**.

| Item | Value |
|------|-------|
| Minecraft | 1.20.1 |
| Forge | 47.3.12+ |
| Java | 17 |
| Package root | `com.corrinedev.tacznpcs` |
| Artifact | `tacz_npc-<mod_version_base>-<craftorio_version>.jar` |

**Fork:** maintained at [craftorio/tacz-npcs](https://github.com/craftorio/tacz-npcs). Original work by Corrinedev.

Main development branch for 1.20.1: `1.20.1`.

## Git and releases

- **Do not open pull requests** against the upstream/source repository (Corrinedev original). Work on the craftorio fork only; push branches and tags to `origin` (`craftorio/tacz-npcs`).
- **Do not commit or push** unless the user explicitly asks.
- **Do not** add `Co-authored-by`, agent names, or AI attribution to commits, docs, release notes, or **code comments**.
- Version lives in `gradle.properties`: `mod_version_base` + `craftorio_version` suffix (full version e.g. `1.0.3-craftorio`). Bump `mod_version_base` when preparing a release.
- Releases are driven by GitHub Actions ([`.github/workflows/release.yml`](.github/workflows/release.yml)):
  - Tag `release/<mod_version>+<craftorio_version>` (e.g. `release/1.0.3+craftorio`), or
  - Tag `<mod_version>` only (e.g. `1.0.3`) — craftorio suffix taken from `gradle.properties`.
- Use descriptive branch names (e.g. `monster-retaliate-1.0.3`), not generic `feature/…` unless the user prefers otherwise.

## Build and run

```bash
./gradlew compileJava    # fast compile check
./gradlew build          # full JAR → build/libs/
./gradlew runClient      # dev client (needs deps in run/mods or Gradle cache)
```

Requires **Java 17**. Mappings: official 1.20.1.

## Architecture

### Entities

| Class | Role |
|-------|------|
| `AbstractScavEntity` | Base NPC: inventory, TACZ gun operator, SmartBrainLib brain, corpse loot (`deadAsContainer`), patrol fields |
| `BanditEntity` | Hostile faction; `MobCategory.MONSTER`; targets players, duty, monsters, villagers, etc. |
| `DutyEntity` | Guard/military faction; `MobCategory.MISC`; targets bandits and monsters; ally logic excludes `MONSTER` category |
| `ScavPlayer` | Experimental fake `ServerPlayer` + `InternalPathfinder` for advanced pathfinding (`/playerscav`) |

NPC combat AI uses **Smart Brain Lib** (sensors + behaviours), not vanilla `GoalSelector`, except for retaliation hooks on vanilla monsters.

### Combat and targeting

| Component | Purpose |
|-----------|---------|
| `NearestTargetOrRetaliate` | Pick nearest visible attackable entity (does not prefer hurt-by over closer targets) |
| `TargetLock` | Short target lock (`LOCK_TICKS`); `retaliate()` sets brain + `Mob.setTarget()` |
| `TaczShootAttack` | Brain behaviour: aim and shoot via `IGunOperator` |
| `NearbyLivingEntitySensor` + predicates | Per-entity-type target detection in `getSensors()` |

When an NPC is damaged, `AbstractScavEntity.hurt()` calls `TargetLock.retaliate()` if the attacker is a different entity type.

**Vanilla monsters → NPCs:** TACZ gun damage does not always set a correct `DamageSource` for vanilla `HurtByTargetGoal`. [`Events.java`](src/main/java/com/corrinedev/tacznpcs/common/Events.java) handles retaliation via `EntityHurtByGunEvent.Post` and `LivingHurtEvent` → `TargetLock.retaliate(monster, scav)`.

### Spawn replacement

[`MonsterReplacement`](src/main/java/com/corrinedev/tacznpcs/common/spawn/MonsterReplacement.java) on `EntityJoinLevelEvent`: pillagers → bandits; Guard Villagers guards → duty (when that mod is present).

### Registries

- `EntityTypeRegistry` — `bandit`, `duty`, `internal_pathfinder`
- `ItemRegistry` — spawn eggs, patch items (rank), bandit/duty patches
- `AttributeRegistry` — entity attributes

### Client

`client/` — GeckoLib-style humanoid renderer, gun renderer, model; `NPCSClient`, `RenderRegistry`.

### Mixins

[`tacz_npc.mixins.json`](src/main/resources/tacz_npc.mixins.json): `FakePlayerJoinMixin` (suppress ScavPlayer join messages), `PlayerRendererMixin` (client), `ConnectionAccessor`.

Register new mixins in that JSON and keep changes minimal.

### Data-driven content

| Path | Purpose |
|------|---------|
| `src/main/resources/data/tacz_npc/tags/items/` | Equipment sets per faction/rank (`bandit_grunt`, `duty_officer`, …) |
| `src/main/resources/data/tacz_npc/loot_tables/` | `bandit.json`, `duty.json` spawn loadouts |
| `src/main/resources/data/minecraft/worldgen/` | Structure overrides (pillager outposts → bandits) |
| `src/main/resources/assets/tacz_npc/lang/` | `en_us.json`, `ru_ru.json` |

Patch items in a set assign rank (Rookie → Expert). See `data/tacz_npc/tags/items/readme-toedit.md`.

### Config

- Server: `config/tacz_npc-common.toml` — damage multipliers, corpse despawn, patrol size, drops
- Client: `config/tacz_npc-client.toml` — name tags

Defined in `Config.java` / `ClientConfig.java`.

## Code conventions

1. **Minimize scope** — smallest correct diff; do not refactor unrelated code.
2. **Match existing style** — package layout, SmartBrainLib patterns, Forge event subscribers, deferred registers.
3. **Prefer extending existing abstractions** — behaviours, sensors, `TargetLock`, loot tables/tags over new parallel systems.
4. **Comments** — only for non-obvious combat AI or TACZ integration details. **Never** mention AI agents, Cursor, Claude, Copilot, or similar in comments (see [.cursor/rules/no-ai-attribution.mdc](.cursor/rules/no-ai-attribution.mdc)).
5. **No new markdown docs** unless the user asks (README, AGENTS.md, etc. are exceptions when requested).
6. **Entity AI changes** — consider both brain (`BrainUtils`, sensors) and edge cases: `deadAsContainer`, `hasLineOfSight`, target lock, ally predicates.
7. **Damage from guns** — if adding NPC↔monster interaction, verify TACZ events and `LivingHurtEvent`; do not assume vanilla hurt attribution works.

## Common tasks

| Task | Where to look |
|------|----------------|
| Change who NPCs attack | `BanditEntity.getSensors()`, `DutyEntity.getSensors()`, `getCoreTasks()` ally/target predicates |
| Change gun/combat behaviour | `TaczShootAttack`, `AbstractScavEntity` fight tasks, `Config` damage multipliers |
| Change loadouts | Item tags + loot tables; `applySpawnLoadout()` |
| Change world spawns | `MonsterReplacement`, datapack worldgen JSON |
| Add lang strings | `assets/tacz_npc/lang/en_us.json`, `ru_ru.json` |
| Version / release | `gradle.properties`, push tag, CI builds release |

## Dependencies (compile)

- Smart Brain Lib (CurseMaven)
- TACZ (CurseMaven)
- MixinExtras (jarJar)
- Dev/runtime: Embeddium, Oculus, PlayerAnimator, Cloth Config (see `build.gradle`)

Do not add dependencies without user approval.

## Testing

No automated test suite. Validate with `./gradlew compileJava` or `./gradlew build`, then manual in-game checks on a dev client or test world.

Typical checks: NPC targets correct faction, gun fire/reload, monster retaliation after shot, corpse inventory, patrol spawn, structure replacement.
