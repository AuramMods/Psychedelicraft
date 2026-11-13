Psychedelicraft
============

[![Current Version](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)](https://img.shields.io/github/v/tag/Sollace/Psychedelicraft)
[![Build Status](https://github.com/Sollace/Psychedelicraft/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/Sollace/Psychedelicraft/actions/workflows/gradle-build.yml)
![Downloads](https://img.shields.io/github/downloads/Sollace/Psychedelicraft/total.svg?color=yellowgreen)
![License](https://img.shields.io/github/license/Sollace/Psychedelicraft)

This branch is developing the Minecraft 1.12.2 Forge port. See the [port report](docs/PORT_12.md) for current feature coverage and test results.

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

Use Minecraft **1.12.2**, **Java 8**, and the tested Forge build **14.23.5.2864**. [Forge 14.23.5.2864 installer](https://maven.minecraftforge.net/net/minecraftforge/forge/1.12.2-14.23.5.2864/forge-1.12.2-14.23.5.2864-installer.jar)

1. Run the Forge installer with **Install client** and select the resulting Forge profile in your launcher. Set that profile's game directory to the instance you want to mod.
2. Copy `psychedelicraft-1.12.2-port.1.jar` into that instance's `mods` folder. The build below produces this jar. Start the Forge profile and confirm Psychedelicraft appears in **Mods**.
3. Create a new 1.12.2 world. The port targets 1.12.2 saves.

Psychedelicraft requires Forge only. For in-game recipe browsing, install the optional **Just Enough Items (JEI) 4.16.1.302 for 1.12.2** in the client's `mods` folder.

For a dedicated server, run the same Forge installer with **Install server** in a new server directory. Copy the same Psychedelicraft jar into its `mods` folder and launch the Forge jar from that directory with Java 8.

```sh
java -jar forge-1.12.2-14.23.5.2864.jar nogui
```

Read and accept the Minecraft EULA in the generated `eula.txt` before starting the server again. Joining clients need the same Psychedelicraft build.

Contributing
============

Use a JDK 8 installation for Gradle. The wrapper builds against Forge `14.23.5.2864` with the `20171003-1.12` MCP mappings.

```sh
./gradlew clean build
./gradlew runClient
./gradlew runServer
```

On Windows, use `gradlew.bat`. The build runs the tests and writes the reobfuscated production jar to `build/libs/psychedelicraft-1.12.2-port.1.jar`. The development client uses `run/client` and the development server uses `run/server`.

On Apple Silicon, run the client with an x86-64 JDK 8 under Rosetta. The macOS LWJGL 2 native library in this Minecraft version requires x86-64.

If a fresh ForgeGradle development run fails in `NetworkRegistry.newChannel` at line 207, run `./gradlew resolveForgeSources` and retry the launch. The mapped sources let ForgeGradle recompile its development runtime. See the port report for the observed failure.
