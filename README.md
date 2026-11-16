Psychedelicraft
============

[![Current Version](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)
[![Build Status](https://github.com/Sollace/Psychedelicraft/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/Sollace/Psychedelicraft/actions/workflows/gradle-build.yml)
![Downloads](https://img.shields.io/github/downloads/Sollace/Psychedelicraft/total.svg?color=yellowgreen)
![License](https://img.shields.io/github/license/Sollace/Psychedelicraft)

This branch is developing the Minecraft 1.20.1 Forge port. The effect registry, initial consumables, ten crop blocks, drying tables, smoking tools and core gameplay hooks are implemented; the full content and audiovisual migration is in progress. See the [1.20.1 port report](docs/PORT_1201.md) for migration coverage and verification.

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

Use Minecraft **1.20.1**, **Java 17** and **Forge 47.4.28**. This branch currently contains the effect foundation, initial consumables, primary crops, coffee roasting, drying-table automation and smoking items/tools. The feature checklist tracks the remaining port.

Build with a Java 17 JDK.

```sh
./gradlew clean build
```

The reobfuscated production jar is `build/libs/psychedelicraft-1.20.1-port.1.jar`. Install the same jar on the Forge client and dedicated server in their `mods` directories. This port targets new 1.20.1 worlds.

Run the unit and headless Forge GameTest suites with `./gradlew build runGameTestServer`.

Development runs use `./gradlew runClient` and `./gradlew runServer`. The background QA workflow is recorded in [the port report](docs/PORT_1201.md).
