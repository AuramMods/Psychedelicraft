# Forge 1.12.2 port investigation

Investigated on **2026-11-11**. Target: Minecraft **1.12.2**, **Forge**, and **Java 8**.

## Recommendation

Use **plain `1.20.1` as the starting point for the easiest modern backport**. It has the modern mod's core gameplay, direct NBT persistence, older rendering interfaces, and substantially less code to translate than `1.21.5`. This is still a substantial platform port: changing the Minecraft version or replacing Fabric's Gradle plugin cannot make it work on 1.12.2.

The new local branch **`codex/1.12.2-forge`** starts at `1.20.1` commit `319a4ea0160d6600cffcd5f5cc9e363ceaab44a1`. This commit adds the investigation only. Its inherited implementation still targets Fabric 1.20.1; a runnable Forge port has not been implemented or tested.

Use **`1.20.1-lts` as the main reference for newer features and fixes**, and `1.21.5` for changes after LTS. If full recent feature parity becomes the overriding requirement, reconsider LTS as the implementation base before doing substantial work. LTS retains Java 17 and the older renderer, but incorporates much of the newer gameplay and an emulation of the component/codec architecture. Starting from plain 1.20.1 prioritizes time to a working port; it does not imply that newer features are permanently excluded.

There is also an old Forge implementation on `master`. Reuse its ideas where helpful, particularly rendering and Forge integration, but do not assume it is a working 1.12 precursor. Its declared build and source APIs disagree. A classic-feature revival from that branch might be competitive, but establishing that would require a separate build/runtime experiment.

## Repository and branch survey

The checkout initially used `1.21.5`; its working tree was clean. `origin` is [AuramMods/Psychedelicraft](https://github.com/AuramMods/Psychedelicraft), and the upstream is [Sollace/Psychedelicraft](https://github.com/Sollace/Psychedelicraft). After fetching origin, all 16 origin branch heads matched upstream's advertised heads. There was no existing 1.12 branch in either branch listing. Searching the fetched commit subjects for `1.12` also returned no matches; this does not rule out work in other forks or unpublished history.

The available branches were:

```text
master
1.19.2  1.19.3  1.19.4
1.20  1.20.1  1.20.1-lts  1.20.2  1.20.2-lts  1.20.4  1.20.4-lts
1.21  1.21.4  1.21.5  1.21.11
25w14craftmine
```

**1.21.5 is the fork's default checkout, but is not its highest numbered Minecraft branch.** `1.21.11` exists and has a newer head. It was inspected for build configuration and scope, not reviewed to the same depth as the two requested candidates.

| Branch | Head | Head date | Declared platform | Main Java files / lines | Assessment |
| --- | --- | --- | --- | ---: | --- |
| `master` | `1f19dfa6` | 2022-12-31 | Forge 1.8, Java 7; legacy APIs remain | 262 / 25,702 | Historical reference; uncertain build coherence |
| `1.19.2` | `8d5b0c9e` | 2023-08-23 | Fabric 1.19.2, Java 17 | 284 / 24,648 | Slightly smaller, but still crosses the same major platform boundaries |
| `1.20.1` | `319a4ea0` | 2024-06-13 | Fabric 1.20.1, Java 17 | 293 / 25,981 | Recommended initial donor |
| `1.20.1-lts` | `720118ae` | 2026-08-17 | Fabric 1.20.1, Java 17 | 433 / 40,650 | Better recent feature coverage; substantially larger port |
| `1.21.5` | `84b6eb29` | 2026-10-07 | Fabric 1.21.5, Java 21 | 433 / 40,527 | More native components, codecs, and renderer changes |
| `1.21.11` | `1636eeec` | 2026-10-12 | Fabric 1.21.11, Java 21 | 448 / 42,099 | Further from the target; reference only |

Dates are Git committer dates, not release dates. Counts cover tracked `src/main/java/**/*.java`; lines include comments and whitespace. They indicate the amount of source to inspect, not a time estimate. Full pinned source snapshots: [plain 1.20.1][plain], [1.20.1 LTS][lts], [1.21.5][modern], [legacy master][legacy], [1.21.11][newest].

### Why plain 1.20.1 beats 1.21.5 for this target

- The plain branch has **140 fewer Java files** and about **36% fewer Java lines**. Its recipe data is already checked in as JSON.
- Player and container persistence is directly expressed in NBT. In 1.21.5, `item/component/PSComponents.java` registers native `ComponentType` values with codecs and packet codecs. That architecture has no native counterpart in 1.12.2.
- Plain `network/MsgDrugProperties.java` carries an entity ID and NBT through Fabwork. The 1.21.5 messages use packet codecs, adding another translation layer before replacing Fabwork with Forge networking.
- Plain rendering uses `PostEffectProcessor`, direct framebuffer operations, and entity-based models. In 1.21.5, `PostEffectRenderer` takes a render `Pool`, `DrugRenderer` consumes `PlayerEntityRenderState` and `RenderTickCounter`, and additional shader pipeline mixins are present. Both need renderer work, but plain 1.20.1 provides fewer intervening abstractions.
- Java 8 still requires substantial rewriting in the plain branch: a textual scan found 37 record declarations there, versus 73 in LTS and 88 in 1.21.5. `var`, pattern matching, switch expressions, and newer library calls also require review. Lowering the compiler target alone is insufficient.

The older `1.19.2` saves only nine Java files and 1,333 lines compared with plain 1.20.1, while still using Fabric, Java 17, modern registries, transfer APIs, and shaders. That small reduction does not justify giving up the later fixes by default.

### What choosing plain 1.20.1 postpones

LTS is much more than a dependency refresh. Its source includes newer fluid pipes, pumps and valves; revised machine processing; richer item/fluid components; additional effect behavior; and expanded configuration, data generation, and rendering compatibility. For example, `GlassTubeBlock`, `PumpBlock`, `ValveBlock`, `FluidTransferUtils`, and `item/component/PSComponents` are absent from plain 1.20.1. Some apparent new classes are refactors of existing features, so file additions alone are not a feature list.

Useful maintenance commits in LTS include:

| Commit | Change to consult when porting the affected system |
| --- | --- |
| `96b26a13` | Bunsen burner item duplication fix |
| `4cad9e64` | Drying table crash during village generation |
| `c0947d68` | Crash when adding items to the vat |
| `a14f8496` | Reacting recipes consuming fluids incorrectly |
| `df47779e` | Crafting table recipe fixes |
| `e6a9a1a6` | Infinite loop while cannabis grows |
| `b847bfeb` | Packet serialization errors |

These are reference fixes, not a claim that every bug exists in the simpler branch: some concern systems introduced or rewritten later. Read each relevant diff and reproduce its behavior on the port before adapting it. Changes beyond LTS include `678cfa56` (dyed bottle crafting), `2fad24c4` (harmonium dyeing/undyeing), and `c7cabe7c` (bottle/cauldron interaction). A wholesale merge from a newer Minecraft branch would also import the platform dependencies being removed.

### What the legacy Forge branch offers

`master/build.gradle` declares Forge `1.8-11.14.3.1491`, ForgeGradle `1.2-SNAPSHOT`, Gradle 2.6, Java 7, and IvToolkit `1.2.1:dev`. However, `PSRegistryHandler` still imports `cpw.mods.fml` and uses APIs such as `setTextureName` and `GameRegistry.registerItem`. There are 57 Java files importing `cpw.mods.fml` and 90 importing IvToolkit. The branch also installs an FML core plugin and ASM transformers under `ivorius.psychedelicraftcore`.

It is therefore an old/incomplete migration reference, not a verified Forge 1.8 baseline. Its `PSRegistryHandler`, proxy classes, fluid classes, world generation, networking setup, and `client/rendering/shaders` can inform a 1.12 implementation. Its old coremod transformers and dependency configuration need fresh review rather than reuse unchanged. No legacy build was attempted during this investigation.

## Architecture and concrete port work

Paths below are relative to `src/main/java/ivorius/psychedelicraft/` on plain 1.20.1 unless another branch is named. These are implementation recommendations derived from source inspection, not completed replacements.

| Area and source touchpoints | Existing behavior | Forge 1.12.2 direction | Relative effort |
| --- | --- | --- | --- |
| `Psychedelicraft`, `client/PsychedelicraftClient`, `PS*` registration classes | Fabric entrypoints, callbacks, registry bootstrap | `@Mod`, Forge lifecycle/events, `RegistryEvent.Register`, separate client proxy | Medium |
| `entity/drug/DrugProperties`, `Drug`, `DrugType`, `influence/*`, `type/*` | Effect progression, attributes, stomach, persistence and sync | Preserve behavior/math; translate Minecraft types and attach player state through a capability | High |
| `mixin/MixinPlayerEntity`, `network/*` | Tick/save/load hooks; copy, join, dimension-change sync; two S2C message types | Capability persistence and Forge events; `SimpleNetworkWrapper` messages with main-thread handling | High |
| `fluid/*`, `fluid/container/*`, `fluid/physical/*` | Custom fluid state, fermentation, containers, Fabric transfer transactions | Forge fluids/`FluidStack`, explicit NBT payloads, `IFluidHandler` and item capabilities | High |
| `block/*`, `block/entity/*` | Crops, storage, processing, multiblocks, placed drinks | `Block`, `IBlockState`, metadata layouts, `TileEntity`, ticking and update packets | High |
| `recipe/*`, `screen/*`, `client/screen/*` | Custom recipe types/serializers, inventory interaction and progress displays | 1.12 recipe factories/`IRecipe`; machine recipe managers; `Container`, `GuiContainer`, `IGuiHandler` | High |
| `client/render/*`, `client/render/shader/*`, `mixin/client/*` | Post effects, world distortion, camera/hand changes, hallucinations and custom models | 1.12 rendering events, `GlStateManager`, framebuffer/shader integration, baked models and TESRs | Very high |
| `world/gen/*`, `entity/PSTradeOffers`, villager mixins | Biome modifications, configured features, structure pools, professions and tasks | Forge world generators, biome selection, legacy village integration and villager AI/trades | High |
| `command/*`, `PSDamageTypes`, `advancement/*` | Brigadier commands, damage registry keys, custom advancement criteria | `CommandBase`/`ICommand`, concrete `DamageSource` instances, 1.12 criteria | Medium |
| `config/*`, `compat/*` | JSON settings, Kirin GUI, Mod Menu and EMI integration | Retain settings semantics with Java 8-compatible serialization; Forge config GUI; later 1.12 recipe-viewer integration | Medium |

Forge's [registry documentation][registries], [capability documentation][capabilities], [networking documentation][networking], and [tile entity documentation][tileentities] describe the target mechanisms. In particular, 1.12 capability data needs explicit client synchronization; attaching persistent state does not replace the existing packets.

### Player state and packets

`DrugProperties.of(PlayerEntity)` currently casts to the mixin-supplied `DrugPropertiesContainer`. `MixinPlayerEntity` supplies tick and NBT hooks and modifies sleep and block-breaking behavior. `Psychedelicraft.onInitialize()` registers copy, join, and world-change callbacks. `sendCapabilities()` sends state to both surrounding players and the owning player; the method's name does not mean it already uses Forge capabilities.

The port must preserve owner and observer updates, tracking-start behavior, death/respawn copy rules, dimension changes, and reconnect persistence. Keep gameplay authoritative on the server and client visual state separated. Replace the two Fabwork messages (`MsgDrugProperties`, `MsgHallucinate`) with explicit message encoders/decoders. Forge 1.12 handlers execute on a network thread by default, so schedule access to game state on the appropriate main thread. [Target networking behavior][networking].

### Fluids and machines

This is more than renaming `FluidVariant` to `FluidStack`. Modern code stores fluid identity plus additional state and uses transactions for inventory/container changes. Forge's fill/drain simulation needs an adapter that preserves quantities, NBT, container remainders, and all-or-nothing changes where required.

Plain `FluidVolumes` already defines a bucket as **1,000 units**, a bottle as 2,000, a barrel as 16,000, and a vat as 32,000. LTS and 1.21.5 instead derive units from Fabric `FluidConstants`, with different barrel/vat capacities. Do not copy newer amounts or recipe timings without choosing the intended gameplay balance and converting units explicitly.

Inspect `Resovoir`, `VariantMarshal`, the fluid container classes, `BarrelBlockEntity`, `MashTubBlockEntity`, `DistilleryBlockEntity`, and `DryingTableBlockEntity` together. Preserve custom fluid data through bottling, pouring, block placement/breaking, saves, hoppers, partial transfers, and external automation. Simulation must not mutate state; refused or partial transfers must not duplicate or destroy contents.

### Blocks and content compatibility

Minecraft 1.12.2 still persists a block's metadata in **four bits**. Each ported block needs an explicit state-to-metadata design. Derive connection/render-only properties from neighbors, or store additional persistent data in a tile entity; use separate block IDs where appropriate. Directly carrying over every modern property combination can silently lose state after a reload. [Forge blockstate documentation][blockstates].

There are also vanilla-content gaps. Plain `PSItems` includes juniper hanging signs, and blocks use newer wood/sound APIs. Hanging signs, chest boats, waterlogging, newer materials, and modern villager job sites cannot be presumed available. Record an explicit substitute, custom implementation, or deferred feature for each such reference instead of silently dropping recipes or registrations.

### Rendering is the biggest technical uncertainty

The mod's defining behavior depends on `DrugRenderer`, `ShaderLoader`, `GeometryShader`, `LoadedShader`, `PostEffectRenderer`, and numerous client mixins. Plain 1.20.1 contains 51 files under its shader directory and 24 shader files declaring GLSL 150. It changes world geometry as well as applying full-screen effects.

1.12's render lifecycle, shader interfaces, vertex submission, and LWJGL environment differ from the modern code. GLSL text and effect formulas are useful inputs, but shader loading, attributes/uniforms, framebuffer ownership, camera transforms, and hook timing need a target-specific implementation. The legacy branch's shader wrappers provide historical reference, not evidence of compatibility with the target.

Prove one full-screen effect and one geometry/camera effect early. Test resolution changes, fullscreen toggles, resource reloads, effect stacking, and cleanup when effects end. A compiling jar or successful title screen would not establish that this part works. Decide whether any remaining hook requires an access transformer or a 1.12-compatible mixin bootstrap after checking actual Forge events; neither the Fabric mixin setup nor the old ASM transformers can be assumed to transfer intact. Optional rendering-mod support comes after a working vanilla Forge renderer and needs separate compatibility testing.

## Dependencies and build system

| Dependency | Plain 1.20.1 | 1.21.5 | Port treatment |
| --- | --- | --- | --- |
| Java | 17 | 21 | Target Java 8 syntax, bytecode and available libraries |
| Gradle / Loom | 8.6 / `1.6-SNAPSHOT` | 8.12 / `1.10-SNAPSHOT` | Replace with a Forge MDK-based build |
| Fabric Loader / API | `0.14.21` / `0.85.0+1.20.1` | `0.16.10` / `0.119.6+1.21.5` | Replace integrations with target Forge/Minecraft APIs |
| Fabwork | `1.2.0` | `1.3.2+1.21` | Replace its packet abstraction |
| Kirin | `1.15.1` | `1.20.5+1.21.5` | Replace GUI helpers; newer branches also use its settings helpers |
| Terraform wood API | `7.0.0-beta.1` | Removed | Reimplement juniper wood/boat/sign integration for 1.12 |
| Mod Menu | `7.0.0-beta.2` | `14.0.0-rc.2` | Replace config entrypoint with Forge integration |
| Recipe viewer | EMI `1.0.19+1.20.1` | TlaApi plus optional EMI/REI paths | Reimplement optional integration against a verified 1.12 viewer API |
| Sodium / Iris | No dedicated compat source in plain branch | Dedicated shader mixins; Sodium enabled, Iris disabled in properties | Do not carry these modern integrations into the baseline |

The listed artifacts are declared for their modern branches; no 1.12 builds of them were established by this audit. Port their small used interfaces or replace the integration rather than assume those jars will load. Also review `org.joml`, Mojang serialization/datafixer utilities, newer Guava methods, and Java library calls: they are not all supplied by the target runtime in the same form.

### Verified Forge starting configuration

The [official 1.12.2 downloads page][forge-downloads] listed **14.23.5.2864** as latest and **14.23.5.2859** as recommended at investigation time. The downloaded [2864 MDK][mdk] was inspected directly. It ships:

- Forge dependency: `net.minecraftforge:forge:1.12.2-14.23.5.2864`.
- ForgeGradle: `net.minecraftforge.gradle:ForgeGradle:3.+`.
- Gradle wrapper: **5.6.4**.
- Java source/target: **1.8**.
- MCP mappings: channel `snapshot`, version `20171003-1.12`.
- IDE tasks documented in its README: `genIntellijRuns` and `genEclipseRuns`.

Use that coherent starting configuration for the first implementation experiment. Run the actual build/client/server tasks and address their concrete failures. Once dependency resolution succeeds, record and pin the resolved ForgeGradle version for repeatability. Do not mix the modern Loom wrapper, an old ForgeGradle 2 tutorial, and this ForgeGradle 3 MDK. These facts establish what the archive contains, not that its build has run successfully on this machine.

Replace `fabric.mod.json` with appropriate 1.12 `mcmod.info`/Forge metadata; review the access widener and mixin hooks individually; provide `pack.mcmeta` for the target. Replace Fabric remapping/publishing tasks, Outlet, and inherited Fabric/Quilt release declarations. Keep publishing credentials and upstream project identifiers out of the new release configuration until the fork's publishing destination is selected.

The inherited CI runs Gradle `check`; modern branches use their modern JDKs. There are no checked-in test files in the reviewed `master`, `1.19.2`, `1.20.1`, LTS, or `1.21.5` snapshots. `1.21.11` adds a client gametest source and metadata, which is not a ready-made 1.12 test harness. Update CI to the target toolchain and actual artifact build during implementation. The `1.21.5` README still says JDK 17 even though its build and CI specify 21, so build files are the more useful source for that branch's declared requirements.

## Assets, recipes, world generation and licensing

Plain 1.20.1 has **996 tracked main resource files**: 758 under `assets`, 234 under `data`, and four top-level files. Its data includes 91 mod recipes, 24 advancements, 45 mod loot tables, six structure templates, custom placed-drink definitions, tags, and auxiliary vanilla-extension worldgen/loot data. LTS and 1.21.5 have fewer checked-in resources because they use Java data-generation providers; that is not evidence of less content.

| Resource type | Reuse and conversion work |
| --- | --- |
| PNG textures, icons, OGG audio | Reuse payloads where appropriate; check paths, atlases, sound declarations and visual alignment in game |
| Model and blockstate JSON | Retain geometry where compatible; translate modern parents, texture references, predicates and state variants; rebuild dynamic drink/fluid models |
| Language JSON | Convert to UTF-8 `assets/psychedelicraft/lang/*.lang`, with translation keys matching target code; preserve formatting and multiline escaping |
| `data/*/recipes` | Convert supported crafting recipes to `assets/<namespace>/recipes`; implement custom factories and machine recipe loading explicitly |
| Tags | Map shared material categories to OreDictionary where suitable; implement other membership lists explicitly |
| Loot and advancements | Move to target-supported resource paths and translate schemas, conditions, functions and criteria; block drops need 1.12 block logic rather than modern block loot tables |
| Worldgen JSON and village structures | Recreate feature placement and legacy village integration; templates may contain modern block names/properties and jigsaw assumptions |
| Shader JSON/GLSL | Preserve effect intent and reusable shader math, but rewrite integration for the target renderer |

Forge 1.12 already supports JSON crafting recipes; it does **not** use the modern datapack layout. Its [recipe documentation][recipes] specifies `assets/<modid>/recipes` and `_factories.json` for custom types. Its [localization documentation][localization] specifies `.lang` files and the escaping rules. Merely moving every `data` file into `assets` will not convert its schemas or implement missing runtime loaders.

The repository's [LICENSE](../LICENSE) is MIT, with a 2022 Lukas Tenbrink copyright notice; older source files also carry attribution. Preserve the license and source notices when porting. The README additionally requests a link and credit to Ivorius. Keep upstream contributor credit, and check licenses independently before copying code from dependencies. This task did not alter attribution or publishing metadata.

## Suggested implementation sequence

1. **Establish the Forge application.** Replace the build with the inspected MDK setup, introduce a minimal Forge entrypoint and sided initialization, and get the packaged jar to load in an actual 1.12.2 client and dedicated server. Adapt or stage incompatible source as explicit work in progress; a stub jar is only a starting milestone.
2. **Prove the distinctive behavior.** Port one consumable, a small effect model, persistent player capability and server-to-client sync. Implement one post effect and one world/camera effect. Confirm behavior with two players, reconnect, death, and a dimension change. This exposes the highest-risk integration before translating hundreds of classes.
3. **Port the core content loop.** Implement crops, harvesting/drying, a container and barrel/mash/distillery chain, recipes, inventories, screens, custom fluids and automation. Verify quantities and NBT through partial transfers, crafting remainders, hoppers, saves and block breaking. Then expand the rest of the existing 1.20.1 content.
4. **Complete acquisition and world behavior.** Port generation, villages/trades, entities, loot, commands, advancements, sounds, translations and settings. Resolve modern-only vanilla references through a documented compatibility list.
5. **Review later improvements and release behavior.** Adapt applicable LTS/newer fixes, then add explicitly selected newer features and optional recipe-viewer/rendering compatibility. Update documentation and CI to the actual supported target. Test the reobfuscated production jar, not only development runs.

For focused automated coverage, prioritize effect progression/decay, NBT round trips, metadata round trips, fluid conservation, simulated operations, and container remainders. In-game checks should cover shader lifecycle, GUI shift-click behavior, multiplayer synchronization, save/reload, generated structures and dedicated-server class loading. These are proposed checks for implemented behavior; none were run as part of this documentation-only investigation.

## Decisions and remaining uncertainty

- **Initial parity:** proposed target is plain 1.20.1 gameplay, followed by selected later fixes/features. Full LTS parity is a larger alternative and would change the base recommendation.
- **Renderer scope:** the desired endpoint includes the characteristic visual effects. Which hooks need custom injection, and which graphics configurations can be supported, remains unproven until the early rendering experiment.
- **Modern-only content:** decide substitutes or custom backports for hanging signs, chest boats and other missing vanilla features as their systems are implemented; track omissions explicitly.
- **Saves:** assume new 1.12.2 worlds. Neither modern-world downgrading nor migration of this mod's modern saves has been designed. Preserve stable registry names where feasible and version the new NBT format.
- **Optional mods and release destination:** exact 1.12 integration versions, graphics-mod compatibility, and fork publishing targets remain to be selected. They are not prerequisites for starting the core Forge port.
- **Effort:** this is a multi-stage rewrite of platform integration, not a version bump. Source inspection supports the donor choice but cannot provide a reliable completion estimate until the first build/rendering/networking experiment runs.

## Investigation record and reproducibility

Completed: fetched origin; listed upstream branches and compared heads; inspected build files, dependencies, entrypoints, player state, networking, fluids, recipes, registries, shaders, mixins, resources, CI and relevant history; examined the legacy Forge source; downloaded and read the current 1.12.2 MDK; cross-checked target APIs against versioned Forge documentation.

Not performed: dependency resolution/builds of the donor branches, a Forge project build, game launches, rendering checks, multiplayer tests, or performance measurements. There is no claim that any inspected branch currently builds. No `docs/MANUAL_CHANGE_PROTOCOL.md` or repository `AGENTS.md` was present in the initial checkout or the selected donor snapshot. No `.codegraph` directory was present in the initial checkout. Temporary source snapshots and the MDK archive were kept outside the repository.

Key full commit IDs, so moving branch tips do not invalidate the investigation:

```text
master       1f19dfa6b72615c00b37d207129196eb621afa83
1.19.2       8d5b0c9e0b0ba5e58554c210c21689d0c6c16210
1.20.1       319a4ea0160d6600cffcd5f5cc9e363ceaab44a1
1.20.1-lts   720118ae1076d00ba2b43327ddb47fa2dffae0ad
1.21.5       84b6eb29fdacd347fe486f560d1cb51a98afaeae
1.21.11      1636eeecdfa46b6c582814461e7eb066e3709b0f
```

Useful read-only commands for continuing the investigation:

```sh
git show 319a4ea0:build.gradle
git show 720118ae:src/main/java/ivorius/psychedelicraft/util/compat/ComponentType.java
git show 84b6eb29:src/main/java/ivorius/psychedelicraft/client/render/shader/PostEffectRenderer.java
git show 1f19dfa6:src/main/java/ivorius/psychedelicraft/PSRegistryHandler.java
git diff --stat 319a4ea0 720118ae
git log --no-merges --oneline 720118ae..84b6eb29
```

The inspected MDK archive's SHA-256 was `bed660e5f8deab6de687d2e894fb489501f00e7a47a9bea260c84a460f8f6733`. This records the downloaded artifact; it is not a build result.

[plain]: https://github.com/Sollace/Psychedelicraft/tree/319a4ea0160d6600cffcd5f5cc9e363ceaab44a1
[lts]: https://github.com/Sollace/Psychedelicraft/tree/720118ae1076d00ba2b43327ddb47fa2dffae0ad
[modern]: https://github.com/Sollace/Psychedelicraft/tree/84b6eb29fdacd347fe486f560d1cb51a98afaeae
[legacy]: https://github.com/Sollace/Psychedelicraft/tree/1f19dfa6b72615c00b37d207129196eb621afa83
[newest]: https://github.com/Sollace/Psychedelicraft/tree/1636eeecdfa46b6c582814461e7eb066e3709b0f
[forge-downloads]: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html
[mdk]: https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2864/forge-1.12.2-14.23.5.2864-mdk.zip
[registries]: https://docs.minecraftforge.net/en/1.12.x/concepts/registries/
[capabilities]: https://docs.minecraftforge.net/en/1.12.x/datastorage/capabilities/
[networking]: https://docs.minecraftforge.net/en/1.12.x/networking/simpleimpl/
[tileentities]: https://docs.minecraftforge.net/en/1.12.x/tileentities/tileentity/
[blockstates]: https://docs.minecraftforge.net/en/1.12.x/blocks/states/
[recipes]: https://docs.minecraftforge.net/en/1.12.x/utilities/recipes/
[localization]: https://docs.minecraftforge.net/en/1.12.x/concepts/internationalization/
