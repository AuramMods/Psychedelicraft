Psychedelicraft
============

[![Current Version](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)
![Downloads](https://img.shields.io/github/downloads/Sollace/Psychedelicraft/total.svg?color=yellowgreen)
![License](https://img.shields.io/github/license/Sollace/Psychedelicraft)

This branch ports Psychedelicraft to Minecraft 1.20.1 Forge. It includes the accepted gameplay changes, native crops and machines, multiplayer effects, and the original audiovisual systems. Build and install instructions are below. The [port report](docs/PORT_1201.md) records the implementation and verification.

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

Use Minecraft **1.20.1**, **Java 17** and **Forge 47.4.10 or later in the 47.x series**. Auram pins 47.4.10, and 47.4.28 remains supported. The [delivery verification](docs/PORT_1201.md#delivery-verification) links the tested gameplay, automation, world generation, client effects and multiplayer behavior.

Run builds, tests, and release packaging on the local development machine with the Gradle wrapper and its cache. GitHub Actions is disabled for this fork.

Build with a Java 17 JDK.

```sh
./gradlew clean build
```

The reobfuscated production jar is `build/libs/psychedelicraft-1.20.1-port.2.jar`. Install the same jar on the Forge client and dedicated server in their `mods` directories. This port targets new 1.20.1 worlds.

Run the unit and headless Forge GameTest suites with `./gradlew build runGameTestServer`. Village graph checks use explicit native Overworld terrain/placement samples, and Harmonium offer checks exercise both configuration states. Reproduction details and remaining test-world outliers are recorded in [the port report](docs/PORT_1201.md).

### In-game guide

Install Patchouli alongside Psychedelicraft. For multiplayer, install it on the client
and dedicated server. Patchouli is optional for the rest of the mod.
[Patchouli 1.20.1-84.1 for Forge](https://www.curseforge.com/minecraft/mc-mods/patchouli/files/6164575)

Craft a **Psychedelicraft Guide** from **one book and one red dye** in any crafting grid.
Hold the red book and use it to open the guide. It covers wild crops, workshop equipment,
drinks, effects, automation, and optional discoveries.
[Guide coverage and verification](docs/PATCHOULI_GUIDE_1201.md)

### Development and gameplay notes

Development runs use `./gradlew runClient` and `./gradlew runServer`. The background QA workflow is recorded in [the port report](docs/PORT_1201.md).

JEI is optional. The integration is tested with **JEI 15.74.0.234 for Forge 1.20.1** and covers drying, mashing, fermentation, maturation, distillation, vessel filling, shears harvesting, fluid-aware crafting remainders, colored bottles and coffee heating. Drying-table recipes support JEI's transfer button and the progress arrow opens their category. Fluid and vessel subtypes preserve brew attributes, warmth and dye. The mod also runs without JEI on both client and dedicated server.

JEI's common and Forge API dependencies are pinned to `15.74.0.234` from [the official Maven repository](https://maven.blamejared.com/mezz/jei/). They are compile-only and are not bundled in the production jar. To include JEI in a developer client, run `./gradlew -PwithJei runClient`; installed clients can add the separate Forge JEI jar to `mods`. The optional dependency range is JEI 15.19 or newer within major version 15; the exact tested version above is recommended.

For multiplayer with the tested JEI version, install that JEI jar on the dedicated server too. Its bundled MezzConfig `0.6.10` configuration channel rejected a client-only installation during the packaged test. Psychedelicraft itself still runs without JEI; this connection limitation belongs to the tested optional dependency.

Hash muffins use six dried cannabis leaves, two wheat and light blue dye. Pour a lava bucket into glass bottles to obtain 125 mB servings, then craft a full lava glass with a water bucket to make an obsidian bottle. This incorporates the glass and returns the empty bucket. Crush the solid bottle into obsidian dust for the bath-salts syringe recipe, or compact nine dust into one obsidian block.

Decorative flower pots accept cannabis, hop, tobacco and coca seeds, or coffea cherries. Use the planting item on an empty vanilla flower pot, then use an empty hand to recover it. Morning glory and juniper saplings also retain their native pot forms.

The 24 mod advancements track plant/material acquisition, drying equipment, smoking items, mash withdrawal, distinct brewed drinks, villager feeding, hangovers and the original sixteen-drug challenge. Brew criteria inspect the actual served contents, including maturation and vinegar state. Minecraft supplies its native trident and Channeling advancements.


The operator command `/hallucinate <type> [targets] [x y z]` creates an owner-only contextual effect. Types are `single_entity`, `multiple_entity`, `friendly_zombies`, `hostile_villagers`, and `rasta_head`, with optional `psychedelicraft:` prefixes. Players may omit targets or supply coordinates relative to their command source; console use needs a player target. Creatures and identity substitutes use native 1.20.1 models without adding real mobs to the world. Reggie's generated dialogue stays in the affected player's local chat.


Drug themes use the strongest enabled active drug and loop locally. Configure `audio.drugsBackgroundMusic` and the per-drug `audio.background_music` flags in the Forge client TOML. Resource packs can supply `psychedelicraft:drug.<drug_path>` sound events; missing or empty sets use the included generic loop. Stimulant breathing follows the player and is audible to nearby observers, while heartbeats stay local. Drug perception also changes existing sound sources and recovers when the effect clears.


Alcohol and kava slur chat, cannabis adds localized filler words, and Zero corrupts text according to the active effect intensity. Set `balancing.messageDistortion.incoming` or `.outgoing` in the local `psychedelicraft-common.toml` to disable either direction. Incoming changes preserve text styling and links. Command input bypasses outgoing distortion, and action-bar messages stay unchanged. Outgoing changes run before Minecraft signs and sends the chat message.

`balancing.enableHarmonium` defaults to `false`, matching the accepted source. Enable it to show its colored creative variants and dealer offers. Existing explicit settings remain respected. The [settings matrix](docs/PORT_1201_SETTINGS.md) maps all 140 Forge config values and their verification evidence.

Open **Mods → Psychedelicraft → Config** for the native Forge settings screen. It includes visual/audio controls, message distortion, gameplay switches, processing times and biome selectors. Changes apply immediately where the underlying system supports them and save when you leave the settings tree. Reset buttons restore the shipped defaults. On a dedicated connection, the screen exposes only local visual/audio/chat choices; edit server gameplay settings on the server. Generation and trade-pool changes require a world restart.

The [native-system mapping report](docs/PORT_1201_NATIVE.md) records how the 1.12 backports map to Minecraft 1.20.1, the tested mod interactions, and the observed mod interactions. CI retains failed-test logs, configs and the owned GameTest world for 14 days to support reproduction.

The paper-bag release check allows native entity visibility callbacks to finish before requiring its exact world count. A separate regression covers accepted drops in an initially unloaded chunk and verifies their count, metadata and identity after loading. The test fixture and reproduction evidence are documented in the port report.

Native conduits recognize the water-like brews for frame activation, player power and hostile targeting. Their normal breathing, mining and gradual underwater vision remain native; agave, kava and bath salts retain their existing non-water classification.

The [current registration and asset matrix](docs/PORT_1201_REGISTRY.md) inventories every running mod item, block, fluid, entity, recipe, command, setting, advancement and drug profile, with source mappings, observed evidence and remaining scope. Its [separate QA sidecar](tools/qa/registration-audit/README.md) reproduces the installed-jar inventory and English/Polish item contact sheets on an isolated display.

The [audiovisual evidence matrix](docs/PORT_1201_RENDERING.md) separates actual dose/packet checks from controlled GPU/model fixtures and links the installed multiplayer lifecycle results. Oculus and modified BSL compatibility testing is deferred.
