Psychedelicraft
============

[![Current Version](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)
[![Build Status](https://github.com/Sollace/Psychedelicraft/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/Sollace/Psychedelicraft/actions/workflows/gradle-build.yml)
![Downloads](https://img.shields.io/github/downloads/Sollace/Psychedelicraft/total.svg?color=yellowgreen)
![License](https://img.shields.io/github/license/Sollace/Psychedelicraft)

This branch is developing the Minecraft 1.20.1 Forge port. The effect registry, initial consumables, ten crop blocks, drying tables, smoking tools, portable fluid vessels, flask/distillery/barrel machines, mash tubs, placed drinks, bottle racks, lattices, grape vines, morning glory, juniper trees/wood/signs/boats, native world fluids, molotovs, Reality Rifts/Rift Jars/Glitch, native dealer/addict professions and trades, farmer crop acquisition, dealer farming/drying/bonemeal work, native village buildings/acquisition loot, hunger perception/overeating, vomiting and paper-bag storage, native post-processing/depth/temporal effects, world/model/cloud/hand deformation, surface coloration, pulses, camera drift/wobble/tremor, mouse inertia, scoped player/hand poses, screen overlays and local heartbeat pulses, contextual entity apparitions/identity swaps/Reggie dialogue, native breathing and drug themes, listener-volume changes, and core gameplay hooks are implemented; the full content and audiovisual migration is in progress. See the [1.20.1 port report](docs/PORT_1201.md) for migration coverage and verification.

The One And Only, accept no substitutes!

 [Wiki](http://minecraft-psychedelicraft.wikia.com) | [Forum](http://www.minecraftforum.net/topic/563257-172-ivorius-mods-drugs-statues-flags-boxes-of-doom-hamsters/) 

----------

Did you ever want to get Steve (or Alex) blackout drunk, stoned, high on crack
or perhaps tripping on shrooms? No? Well, I've got the perfect mod fo... Wait a minute...

Anyway, this mod allows you to do all these things. It utilizes shaders and is
thus able to accomplish a party of colors and effects you may not have seen before.
You (most likely) won't be disappointed.

Notes And References
===========
 - By downloading, you confirm you are of a legal age to view this kind of content: Fictional drug abuse. That is usually 18 or older.
 - Psychedelicraft may not work on every graphics card. If it doesn't (and you have weird visual glitches), please send me the runtime log and screenshots.

Granted Attribution
============

This mod was released by Ivorious under the MIT license, as stipulated by them:
 - Link to this page
 - and mention the creator's/maintainers (Ivoriou's) name)

and you may use this mod in modpacks.

Installation
============

Use Minecraft **1.20.1**, **Java 17** and **Forge 47.4.28**. This branch currently contains the effect foundation, initial consumables, primary crops, coffee roasting, drying-table automation, smoking items/tools, portable drinks, fluid transfers, coffee servings and flask/distillery/barrel processing, mash-tub stewing and fermentation, placed vessels, bottle racks, decorative lab props, native lattices/vines, morning-glory extracts and concentrated-extract LSD paper crafting, and the juniper tree/fruit/wood family with native signs, boats, chest boats and berry mash. Native world-fluid placement, metadata propagation, buckets, dispensers, coffee cooling and vat inflow are also implemented, along with molotov crafting, fluid-preserving bottle conversion and native projectile behavior. Reality Rifts, Rift Jar capture/discharge/overload and native Glitch behavior are implemented with their core rendering assets. Native dealer/addict professions, drying-table job acquisition, original trade pools, farmer grape acquisition and muffin feeding are also implemented. Dealer pickup, nine-input drying, crop planting/harvesting and bonemeal work are implemented with native villager AI and work-item cleanup. Native plains/desert village buildings and additive village/dungeon acquisition loot are implemented. Drug hunger perception, overeating, caught/body vomiting, paper-bag inventory/world/dispenser storage and the bag-of-vomit disguise are also implemented. The feature checklist tracks the remaining port.

Build with a Java 17 JDK.

```sh
./gradlew clean build
```

The reobfuscated production jar is `build/libs/psychedelicraft-1.20.1-port.1.jar`. Install the same jar on the Forge client and dedicated server in their `mods` directories. This port targets new 1.20.1 worlds.

Run the unit and headless Forge GameTest suites with `./gradlew build runGameTestServer`.

Development runs use `./gradlew runClient` and `./gradlew runServer`. The background QA workflow is recorded in [the port report](docs/PORT_1201.md).

JEI is optional. The integration is tested with **JEI 15.74.0.234 for Forge 1.20.1** and covers drying, mashing, fermentation, maturation, distillation, vessel filling, shears harvesting, fluid-aware crafting remainders, colored bottles and coffee heating. Drying-table recipes support JEI's transfer button and the progress arrow opens their category. Fluid and vessel subtypes preserve brew attributes, warmth and dye. The mod also runs without JEI on both client and dedicated server.

JEI's common and Forge API dependencies are pinned to `15.74.0.234` from [the official Maven repository](https://maven.blamejared.com/mezz/jei/). They are compile-only and are not bundled in the production jar. To include JEI in a developer client, run `./gradlew -PwithJei runClient`; installed clients can add the separate Forge JEI jar to `mods`. The optional dependency range is JEI 15.19 or newer within major version 15; the exact tested version above is recommended.


The operator command `/hallucinate <type> [targets] [x y z]` creates an owner-only contextual effect. Types are `single_entity`, `multiple_entity`, `friendly_zombies`, `hostile_villagers`, and `rasta_head`, with optional `psychedelicraft:` prefixes. Players may omit targets or supply coordinates relative to their command source; console use needs a player target. Creatures and identity substitutes use native 1.20.1 models without adding real mobs to the world. Reggie's generated dialogue stays in the affected player's local chat.


Drug themes use the strongest enabled active drug and loop locally. Configure `audio.drugsBackgroundMusic` and the per-drug `audio.background_music` flags in the Forge client TOML. Resource packs can supply `psychedelicraft:drug.<drug_path>` sound events; missing or empty sets use the included generic loop. Stimulant breathing follows the player and is audible to nearby observers, while heartbeats stay local. Drug perception also changes existing sound sources and recovers when the effect clears.
