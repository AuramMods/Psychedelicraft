# Current Forge 1.20.1 registration and asset matrix

This is the C35 runtime inventory, replacing the historical **Initial registered-content inventory** as the current registration view. It describes the production jar loaded on installed Forge, rather than counting declarations as runtime proof. All mod IDs below have the `psychedelicraft:` namespace unless another namespace is shown.

Minecraft **1.20.1**, Forge **47.4.28**; production SHA-256 **`edcecfa9a1918227be345f583cbd389415bf2644a49d44f57c6afdd9157466c5`**. The production source is C34 (`388fd078`); C35 adds the separate QA sidecar and this evidence. Final runtime snapshots and four contact sheets are retained in ignored `run/production/cycle35/game-final/registration-audit`.

The [reproducible audit tool](../tools/qa/registration-audit/README.md) exports actual registry values, loaded recipes, advancements, commands and drug types, then calls native model baking/selection and GUI item rendering. It repeats the client observations after a real Polish language/resource reload. C35 proves registration/assets/default names; behavior evidence is separately assigned below. It does not prove every NBT subtype, all block-state pixels, natural acquisition, or multiplayer lifecycle paths.

## Sources and status

[Accepted Forge source](https://github.com/AuramMods/Psychedelicraft/tree/51286f8f3e6866e8b952175c9c60c7f6e63bef8d/src/main/java/ivorius/psychedelicraft/) supplies custom behavior and fixes; [original 1.20.1 source](https://github.com/AuramMods/Psychedelicraft/tree/319a4ea0160d6600cffcd5f5cc9e363ceaab44a1/src/main/java/ivorius/psychedelicraft/) supplies the complete modern baseline. Chosen implementation links identify the current Java class or its native registration owner. C-number evidence refers to [the cycle report](PORT_1201.md). [Native mappings](PORT_1201_NATIVE.md), [all 140 settings](PORT_1201_SETTINGS.md), and [audiovisual evidence](PORT_1201_RENDERING.md) are part of this matrix.

This inventory retains its C35 snapshot provenance. C36 verifies combined rendering, C37 verifies the two-player lifecycle, and C38–C39 verify native and wild acquisition. The main report links these later checks. Oculus and modified BSL compatibility testing is deferred.

| Runtime registry | Mod entries |
| --- | --- |
| `BLOCKS` | 86 |
| `FLUIDS` | 54 |
| `ITEMS` | 100 |
| `SOUND_EVENTS` | 8 |
| `ENTITY_TYPES` | 4 |
| `BLOCK_ENTITY_TYPES` | 12 |
| `PARTICLE_TYPES` | 2 |
| `MENU_TYPES` | 2 |
| `RECIPE_TYPES` | 2 |
| `RECIPE_SERIALIZERS` | 10 |
| `VILLAGER_PROFESSIONS` | 2 |
| `POI_TYPES` | 1 |
| `FEATURES` | 1 |
| `FLUID_TYPES` | 27 |
| Loaded mod recipes | 94 |
| Advancements | 24 |
| Mod drug profiles (not Forge potions) | 18 |
| Operator command roots | 3 |
| Forge settings | 140; enumerated in linked settings matrix |
| Advancement criteria | 4 |

## BLOCKS

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `acacia_barrel` | [BarrelBlock](../src/main/java/ivorius/psychedelicraft/block/BarrelBlock.java) | C7 processing/automation; C6 contents; native `ENTITYBLOCK_ANIMATED` render path |
| `agave` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `agave_plant` | [AgavePlantBlock](../src/main/java/ivorius/psychedelicraft/block/AgavePlantBlock.java) | C3 placement and growth, C39 native wild generation; C35 6 states, 6 MODEL states baked |
| `apple` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `banana` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `bath_salts` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `belladonna` | [NightshadeBlock](../src/main/java/ivorius/psychedelicraft/block/NightshadeBlock.java) | C3 placement and growth, C39 native wild generation; C35 8 states, 8 MODEL states baked |
| `belladonna_extract` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `birch_barrel` | [BarrelBlock](../src/main/java/ivorius/psychedelicraft/block/BarrelBlock.java) | C7 processing/automation; C6 contents; native `ENTITYBLOCK_ANIMATED` render path |
| `bottle_rack` | [BottleRackBlock](../src/main/java/ivorius/psychedelicraft/block/BottleRackBlock.java) | C9 display/props/rack; C32 exact display drops; C35 4 states, 4 MODEL states baked |
| `bunsen_burner` | [LabPropBlock](../src/main/java/ivorius/psychedelicraft/block/LabPropBlock.java) | C9 display/props/rack; C32 exact display drops; C35 1 states, 1 MODEL states baked |
| `caffeine` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `cannabis` | [CannabisPlantBlock](../src/main/java/ivorius/psychedelicraft/block/CannabisPlantBlock.java) | C3 placement and growth, C39 native wild generation; C35 64 states, 64 MODEL states baked |
| `cannabis_tea` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `coca` | [CocaPlantBlock](../src/main/java/ivorius/psychedelicraft/block/CocaPlantBlock.java) | C3 placement and growth, C39 native wild generation; C35 52 states, 52 MODEL states baked |
| `coca_tea` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `cocaine` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `coffea` | [CoffeaPlantBlock](../src/main/java/ivorius/psychedelicraft/block/CoffeaPlantBlock.java) | C3 placement and growth, C39 native wild generation; C35 64 states, 64 MODEL states baked |
| `coffee` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `corn` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `dark_oak_barrel` | [BarrelBlock](../src/main/java/ivorius/psychedelicraft/block/BarrelBlock.java) | C7 processing/automation; C6 contents; native `ENTITYBLOCK_ANIMATED` render path |
| `distillery` | [DistilleryBlock](../src/main/java/ivorius/psychedelicraft/block/DistilleryBlock.java) | C7 processing/automation; C6 contents; C35 6 states, 6 MODEL states baked |
| `drying_table` | [DryingTableBlock](../src/main/java/ivorius/psychedelicraft/block/DryingTableBlock.java) | C4 drying/sided automation; C35 1 states, 1 MODEL states baked |
| `flask` | [FlaskBlock](../src/main/java/ivorius/psychedelicraft/block/FlaskBlock.java) | C7 processing/automation; C6 contents; C35 1 states, 1 MODEL states baked |
| `fruiting_juniper_leaves` | [JuniperLeavesBlock](../src/main/java/ivorius/psychedelicraft/block/JuniperLeavesBlock.java) | C11 wood/fruit/vehicle integration; C35 28 states, 28 MODEL states baked |
| `glitch` | [GlitchedBlock](../src/main/java/ivorius/psychedelicraft/block/GlitchedBlock.java) | C14 rifts/jars/glitch; C36 combined native rendering/reload/resize; C35 1 states, 1 MODEL states baked |
| `honey` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `hop` | [HopPlantBlock](../src/main/java/ivorius/psychedelicraft/block/HopPlantBlock.java) | C3 placement and growth, C39 native wild generation; C35 64 states, 64 MODEL states baked |
| `iron_drying_table` | [DryingTableBlock](../src/main/java/ivorius/psychedelicraft/block/DryingTableBlock.java) | C4 drying/sided automation; C35 1 states, 1 MODEL states baked |
| `jimsonweed` | [NightshadeBlock](../src/main/java/ivorius/psychedelicraft/block/NightshadeBlock.java) | C3 placement and growth, C39 native wild generation; C35 8 states, 8 MODEL states baked |
| `jimsonweed_extract` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `jungle_barrel` | [BarrelBlock](../src/main/java/ivorius/psychedelicraft/block/BarrelBlock.java) | C7 processing/automation; C6 contents; native `ENTITYBLOCK_ANIMATED` render path |
| `juniper` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `juniper_button` | Native `ButtonBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 24 states, 24 MODEL states baked |
| `juniper_door` | Native `DoorBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 64 states, 64 MODEL states baked |
| `juniper_fence` | Native `FenceBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 32 states, 32 MODEL states baked |
| `juniper_fence_gate` | Native `FenceGateBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 32 states, 32 MODEL states baked |
| `juniper_hanging_sign` | [JuniperSigns$Hanging](../src/main/java/ivorius/psychedelicraft/block/JuniperSigns.java) | C11 wood/fruit/vehicle integration; native `INVISIBLE` render path |
| `juniper_leaves` | [JuniperLeavesBlock](../src/main/java/ivorius/psychedelicraft/block/JuniperLeavesBlock.java) | C11 wood/fruit/vehicle integration; C35 28 states, 28 MODEL states baked |
| `juniper_log` | [JuniperLogBlock](../src/main/java/ivorius/psychedelicraft/block/JuniperLogBlock.java) | C11 wood/fruit/vehicle integration; C35 3 states, 3 MODEL states baked |
| `juniper_planks` | Native `Block` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 1 states, 1 MODEL states baked |
| `juniper_pressure_plate` | Native `PressurePlateBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 2 states, 2 MODEL states baked |
| `juniper_sapling` | [JuniperSaplingBlock](../src/main/java/ivorius/psychedelicraft/block/JuniperSaplingBlock.java) | C11 wood/fruit/vehicle integration; C35 2 states, 2 MODEL states baked |
| `juniper_sign` | [JuniperSigns$Standing](../src/main/java/ivorius/psychedelicraft/block/JuniperSigns.java) | C11 wood/fruit/vehicle integration; native `INVISIBLE` render path |
| `juniper_slab` | Native `SlabBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 6 states, 6 MODEL states baked |
| `juniper_stairs` | Native `StairBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 80 states, 80 MODEL states baked |
| `juniper_trapdoor` | Native `TrapDoorBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 64 states, 64 MODEL states baked |
| `juniper_wall_hanging_sign` | [JuniperSigns$HangingWall](../src/main/java/ivorius/psychedelicraft/block/JuniperSigns.java) | C11 wood/fruit/vehicle integration; native `INVISIBLE` render path |
| `juniper_wall_sign` | [JuniperSigns$Wall](../src/main/java/ivorius/psychedelicraft/block/JuniperSigns.java) | C11 wood/fruit/vehicle integration; native `INVISIBLE` render path |
| `juniper_wood` | [JuniperLogBlock](../src/main/java/ivorius/psychedelicraft/block/JuniperLogBlock.java) | C11 wood/fruit/vehicle integration; C35 3 states, 3 MODEL states baked |
| `kava` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `lattice` | [LatticeBlock](../src/main/java/ivorius/psychedelicraft/block/LatticeBlock.java) | C10 vine/flower integration; C33 exact harvests; C35 32 states, 32 MODEL states baked |
| `mash_tub` | [MashTubBlock](../src/main/java/ivorius/psychedelicraft/block/MashTubBlock.java) | C8 batches/walls/automation; C35 144 states, 144 MODEL states baked |
| `mash_tub_edge` | [MashTubWallBlock](../src/main/java/ivorius/psychedelicraft/block/MashTubWallBlock.java) | C8 batches/walls/automation; native `INVISIBLE` render path |
| `milk` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `morning_glory` | [VineStemBlock](../src/main/java/ivorius/psychedelicraft/block/VineStemBlock.java) | C10 vine/flower integration; C33 exact harvests; C35 80 states, 80 MODEL states baked |
| `morning_glory_extract` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `morning_glory_lattice` | [LatticeCropBlock](../src/main/java/ivorius/psychedelicraft/block/LatticeCropBlock.java) | C10 vine/flower integration; C33 exact harvests; C35 256 states, 256 MODEL states baked |
| `oak_barrel` | [BarrelBlock](../src/main/java/ivorius/psychedelicraft/block/BarrelBlock.java) | C7 processing/automation; C6 contents; native `ENTITYBLOCK_ANIMATED` render path |
| `peyote` | [PeyoteBlock](../src/main/java/ivorius/psychedelicraft/block/PeyoteBlock.java) | C3 placement and growth, C39 native wild generation; C35 4 states, 4 MODEL states baked |
| `peyote_juice` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `pineapple` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `placed_drink` | [PlacedDrinksBlock](../src/main/java/ivorius/psychedelicraft/block/PlacedDrinksBlock.java) | C9 display/props/rack; C32 exact display drops; native `INVISIBLE` render path |
| `potato` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `potted_cannabis` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `potted_coca` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `potted_coffea` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `potted_hop` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `potted_juniper_sapling` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `potted_morning_glory` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `potted_tobacco` | Native `FlowerPotBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C10/C11 native pots; C26 all five drug pots; C35 1 states, 1 MODEL states baked |
| `red_grapes` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `rice` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `rift_jar` | [RiftJarBlock](../src/main/java/ivorius/psychedelicraft/block/RiftJarBlock.java) | C14 rifts/jars/glitch; C36 combined native rendering/reload/resize; native `ENTITYBLOCK_ANIMATED` render path |
| `slurry` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `spruce_barrel` | [BarrelBlock](../src/main/java/ivorius/psychedelicraft/block/BarrelBlock.java) | C7 processing/automation; C6 contents; native `ENTITYBLOCK_ANIMATED` render path |
| `stripped_juniper_log` | Native `RotatedPillarBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 3 states, 3 MODEL states baked |
| `stripped_juniper_wood` | Native `RotatedPillarBlock` via [PSBlocks](../src/main/java/ivorius/psychedelicraft/block/PSBlocks.java) | C11 wood/fruit/vehicle integration; C35 3 states, 3 MODEL states baked |
| `sugar_cane` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `tobacco` | [TobaccoPlantBlock](../src/main/java/ivorius/psychedelicraft/block/TobaccoPlantBlock.java) | C3 placement and growth, C39 native wild generation; C35 64 states, 64 MODEL states baked |
| `tomato` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `tomatoes` | [NightshadeBlock](../src/main/java/ivorius/psychedelicraft/block/NightshadeBlock.java) | C3 placement and growth, C39 native wild generation; C35 8 states, 8 MODEL states baked |
| `tray` | [LabPropBlock$Tray](../src/main/java/ivorius/psychedelicraft/block/LabPropBlock.java) | C9 display/props/rack; C32 exact display drops; C35 2 states, 2 MODEL states baked |
| `wheat` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `wheat_hop` | [PhysicalFluidBlock](../src/main/java/ivorius/psychedelicraft/block/PhysicalFluidBlock.java) | C12 world fluid/metadata; C34 conduit classification; native `INVISIBLE` render path |
| `wine_grape_lattice` | [LatticeCropBlock](../src/main/java/ivorius/psychedelicraft/block/LatticeCropBlock.java) | C10 vine/flower integration; C33 exact harvests; C35 256 states, 256 MODEL states baked |

## FLUIDS

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `agave` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `agave`, water tag `false` |
| `apple` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `apple`, water tag `true` |
| `banana` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `banana`, water tag `true` |
| `bath_salts` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `bath_salts`, water tag `false` |
| `belladonna_extract` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `belladonna_extract`, water tag `true` |
| `caffeine` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `caffeine`, water tag `true` |
| `cannabis_tea` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `cannabis_tea`, water tag `true` |
| `coca_tea` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `coca_tea`, water tag `true` |
| `cocaine` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `cocaine`, water tag `true` |
| `coffee` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `coffee`, water tag `true` |
| `corn` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `corn`, water tag `true` |
| `flowing_agave` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `agave`, water tag `false` |
| `flowing_apple` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `apple`, water tag `true` |
| `flowing_banana` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `banana`, water tag `true` |
| `flowing_bath_salts` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `bath_salts`, water tag `false` |
| `flowing_belladonna_extract` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `belladonna_extract`, water tag `true` |
| `flowing_caffeine` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `caffeine`, water tag `true` |
| `flowing_cannabis_tea` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `cannabis_tea`, water tag `true` |
| `flowing_coca_tea` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `coca_tea`, water tag `true` |
| `flowing_cocaine` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `cocaine`, water tag `true` |
| `flowing_coffee` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `coffee`, water tag `true` |
| `flowing_corn` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `corn`, water tag `true` |
| `flowing_honey` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `honey`, water tag `true` |
| `flowing_jimsonweed_extract` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `jimsonweed_extract`, water tag `true` |
| `flowing_juniper` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `juniper`, water tag `true` |
| `flowing_kava` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `kava`, water tag `false` |
| `flowing_milk` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `milk`, water tag `true` |
| `flowing_morning_glory_extract` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `morning_glory_extract`, water tag `true` |
| `flowing_peyote_juice` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `peyote_juice`, water tag `true` |
| `flowing_pineapple` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `pineapple`, water tag `true` |
| `flowing_potato` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `potato`, water tag `true` |
| `flowing_red_grapes` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `red_grapes`, water tag `true` |
| `flowing_rice` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `rice`, water tag `true` |
| `flowing_slurry` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `slurry`, water tag `true` |
| `flowing_sugar_cane` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `sugar_cane`, water tag `true` |
| `flowing_tomato` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `tomato`, water tag `true` |
| `flowing_wheat` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `wheat`, water tag `true` |
| `flowing_wheat_hop` | [WorldFluid$Flowing](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `wheat_hop`, water tag `true` |
| `honey` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `honey`, water tag `true` |
| `jimsonweed_extract` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `jimsonweed_extract`, water tag `true` |
| `juniper` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `juniper`, water tag `true` |
| `kava` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `kava`, water tag `false` |
| `milk` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `milk`, water tag `true` |
| `morning_glory_extract` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `morning_glory_extract`, water tag `true` |
| `peyote_juice` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `peyote_juice`, water tag `true` |
| `pineapple` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `pineapple`, water tag `true` |
| `potato` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `potato`, water tag `true` |
| `red_grapes` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `red_grapes`, water tag `true` |
| `rice` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `rice`, water tag `true` |
| `slurry` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `slurry`, water tag `true` |
| `sugar_cane` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `sugar_cane`, water tag `true` |
| `tomato` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `tomato`, water tag `true` |
| `wheat` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `wheat`, water tag `true` |
| `wheat_hop` | [WorldFluid$Source](../src/main/java/ivorius/psychedelicraft/fluid/WorldFluid.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration; source `wheat_hop`, water tag `true` |

## ITEMS

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `acacia_barrel` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `agave_leaf` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `bag_o_vomit` | [BagOfVomitItem](../src/main/java/ivorius/psychedelicraft/item/BagOfVomitItem.java) | C18 stomach/bag/disguise; C33 native item visibility; C35 EN/PL native default render/name: present |
| `belladonna_berries` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `belladonna_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `belladonna_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `birch_barrel` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `bong` | [BongItem](../src/main/java/ivorius/psychedelicraft/item/BongItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `bottle` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `bottle_rack` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C9 display/props/rack; C32 exact display drops; C35 EN/PL native default render/name: present |
| `brown_magic_mushrooms` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C1/C2 scalar/use benefits; C4 drying; C10 LSD paper; C35 EN/PL native default render/name: present |
| `bunsen_burner` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C9 display/props/rack; C32 exact display drops; C35 EN/PL native default render/name: present |
| `cannabis_buds` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `cannabis_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `cannabis_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `cigar` | [SmokeableItem](../src/main/java/ivorius/psychedelicraft/item/SmokeableItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `cigarette` | [SmokeableItem](../src/main/java/ivorius/psychedelicraft/item/SmokeableItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `coca_leaves` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `coca_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `cocaine_powder` | [PowderItem](../src/main/java/ivorius/psychedelicraft/item/PowderItem.java) | C1/C2 scalar/use benefits; C4 drying; C10 LSD paper; C35 EN/PL native default render/name: present |
| `coffea_cherries` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `coffee_beans` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `dark_oak_barrel` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `distillery` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `dried_belladonna_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 material source; C4 drying ingredient/output; C35 EN/PL native default render/name: present |
| `dried_cannabis_buds` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 material source; C4 drying ingredient/output; C35 EN/PL native default render/name: present |
| `dried_cannabis_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 material source; C4 drying ingredient/output; C35 EN/PL native default render/name: present |
| `dried_coca_leaves` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 material source; C4 drying ingredient/output; C35 EN/PL native default render/name: present |
| `dried_jimsonweed_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 material source; C4 drying ingredient/output; C35 EN/PL native default render/name: present |
| `dried_peyote` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C1/C2 scalar/use benefits; C4 drying; C10 LSD paper; C35 EN/PL native default render/name: present |
| `dried_tobacco` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 material source; C4 drying ingredient/output; C35 EN/PL native default render/name: present |
| `drying_table` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C4 drying/sided automation; C35 EN/PL native default render/name: present |
| `filled_bowl` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `filled_bucket` | [FilledBucketItem](../src/main/java/ivorius/psychedelicraft/item/FilledBucketItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `filled_glass_bottle` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `flask` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `fruiting_juniper_leaves` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `glass_chalice` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `harmonium` | [HarmoniumItem](../src/main/java/ivorius/psychedelicraft/item/HarmoniumItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `hash_muffin` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C2 consumption; C28 acquisition/container recipes; C35 EN/PL native default render/name: present |
| `hop_cones` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `hop_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `iron_drying_table` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C4 drying/sided automation; C35 EN/PL native default render/name: present |
| `jimsonweed_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `jimsonweed_seed_pod` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `jimsonweed_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `joint` | [SmokeableItem](../src/main/java/ivorius/psychedelicraft/item/SmokeableItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `jolly_rancher` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C1/C2 scalar/use benefits; C4 drying; C10 LSD paper; C35 EN/PL native default render/name: present |
| `jungle_barrel` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `juniper_berries` | [QuickFoodItem](../src/main/java/ivorius/psychedelicraft/item/QuickFoodItem.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_boat` | [JuniperBoatItem](../src/main/java/ivorius/psychedelicraft/item/JuniperBoatItem.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_button` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_chest_boat` | [JuniperBoatItem](../src/main/java/ivorius/psychedelicraft/item/JuniperBoatItem.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_door` | Native `DoubleHighBlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_fence` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_fence_gate` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_hanging_sign` | Native `HangingSignItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_leaves` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_log` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_planks` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_pressure_plate` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_sapling` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_sign` | Native `SignItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_slab` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_stairs` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_trapdoor` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `juniper_wood` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `lattice` | [PSItems$1](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C10 vine/flower integration; C33 exact harvests; C35 EN/PL native default render/name: present |
| `lsd_pill` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C2 consumption; C28 acquisition/container recipes; C35 EN/PL native default render/name: present |
| `lsd_square` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C1/C2 scalar/use benefits; C4 drying; C10 LSD paper; C35 EN/PL native default render/name: present |
| `mash_tub` | [MashTubItem](../src/main/java/ivorius/psychedelicraft/item/MashTubItem.java) | C8 batches/walls/automation; C35 EN/PL native default render/name: present |
| `molotov_cocktail` | [MolotovCocktailItem](../src/main/java/ivorius/psychedelicraft/item/MolotovCocktailItem.java) | C13 native projectile and fluid-preserving conversion; C35 EN/PL native default render/name: present |
| `morning_glory` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C10 vine/flower integration; C33 exact harvests; C35 EN/PL native default render/name: present |
| `morning_glory_lattice` | [LatticeCropItem](../src/main/java/ivorius/psychedelicraft/item/LatticeCropItem.java) | C10 vine/flower integration; C33 exact harvests; C35 EN/PL native default render/name: present |
| `morning_glory_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C10 vine/flower integration; C33 exact harvests; C35 EN/PL native default render/name: present |
| `oak_barrel` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `obsidian_bottle` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C2 consumption; C28 acquisition/container recipes; C35 EN/PL native default render/name: present |
| `obsidian_dust` | [PowderItem](../src/main/java/ivorius/psychedelicraft/item/PowderItem.java) | C2 consumption; C28 acquisition/container recipes; C35 EN/PL native default render/name: present |
| `paper_bag` | [PaperBagItem](../src/main/java/ivorius/psychedelicraft/item/PaperBagItem.java) | C18 stomach/bag/disguise; C33 native item visibility; C35 EN/PL native default render/name: present |
| `peyote` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 placement and growth, C39 native wild generation; C35 EN/PL native default render/name: present |
| `peyote_joint` | [SmokeableItem](../src/main/java/ivorius/psychedelicraft/item/SmokeableItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `red_magic_mushrooms` | [EdibleItem](../src/main/java/ivorius/psychedelicraft/item/EdibleItem.java) | C1/C2 scalar/use benefits; C4 drying; C10 LSD paper; C35 EN/PL native default render/name: present |
| `rift_jar` | [RiftJarItem](../src/main/java/ivorius/psychedelicraft/item/RiftJarItem.java) | C14 rifts/jars/glitch; C36 combined native rendering/reload/resize; C35 EN/PL native default render/name: present |
| `shot_glass` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `smoking_pipe` | [BongItem](../src/main/java/ivorius/psychedelicraft/item/BongItem.java) | C5 smoking/stacking; C30 optional Harmonium trades; C35 EN/PL native default render/name: present |
| `spruce_barrel` | [FluidMachineItem](../src/main/java/ivorius/psychedelicraft/item/FluidMachineItem.java) | C7 processing/automation; C6 contents; C35 EN/PL native default render/name: present |
| `stone_cup` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `stripped_juniper_log` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `stripped_juniper_wood` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C11 wood/fruit/vehicle integration; C35 EN/PL native default render/name: present |
| `syringe` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |
| `tobacco` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 placement and growth, C39 native wild generation; C35 EN/PL native default render/name: present |
| `tobacco_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `tomato` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `tomato_leaf` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `tomato_seeds` | [CropSeedItem](../src/main/java/ivorius/psychedelicraft/item/CropSeedItem.java) | C3 crops/materials/roasting; C4/C6 processing inputs; C35 EN/PL native default render/name: present |
| `tray` | Native `BlockItem` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C9 display/props/rack; C32 exact display drops; C35 EN/PL native default render/name: present |
| `vomit` | Native `Item` via [PSItems](../src/main/java/ivorius/psychedelicraft/item/PSItems.java) | C18 stomach/bag/disguise; C33 native item visibility; C35 EN/PL native default render/name: present |
| `wine_grape_lattice` | [LatticeCropItem](../src/main/java/ivorius/psychedelicraft/item/LatticeCropItem.java) | C10 vine/flower integration; C33 exact harvests; C35 EN/PL native default render/name: present |
| `wine_grapes` | [WineGrapesItem](../src/main/java/ivorius/psychedelicraft/item/WineGrapesItem.java) | C10 vine/flower integration; C33 exact harvests; C35 EN/PL native default render/name: present |
| `wooden_mug` | [PortableFluidItem](../src/main/java/ivorius/psychedelicraft/item/PortableFluidItem.java) | C6 transfer/servings; C12 world buckets; C35 EN/PL native default render/name: present |

## SOUND_EVENTS

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `block.bottle_rack.use` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `block.placed_drink.set_down` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `block.rift_jar.close` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `block.rift_jar.open` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `block.rift_jar.toggle` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `drug.generic` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `entity.player.breath` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |
| `entity.player.heartbeat` | Native `SoundEvent` via [PSSounds](../src/main/java/ivorius/psychedelicraft/PSSounds.java) | C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory |

## ENTITY_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `juniper_boat` | Native `EntityType` via [PSEntities](../src/main/java/ivorius/psychedelicraft/entity/PSEntities.java) | C11 wood/fruit/vehicle integration; C35 renderer `JuniperBoatRenderer` |
| `juniper_chest_boat` | Native `EntityType` via [PSEntities](../src/main/java/ivorius/psychedelicraft/entity/PSEntities.java) | C11 wood/fruit/vehicle integration; C35 renderer `JuniperBoatRenderer` |
| `molotov_cocktail` | Native `EntityType` via [PSEntities](../src/main/java/ivorius/psychedelicraft/entity/PSEntities.java) | C13 native projectile and fluid-preserving conversion; C35 renderer `ThrownItemRenderer` |
| `reality_rift` | Native `EntityType` via [PSEntities](../src/main/java/ivorius/psychedelicraft/entity/PSEntities.java) | C14 rifts/jars/glitch; C36 combined native rendering/reload/resize; C35 renderer `RealityRiftRenderer` |

## BLOCK_ENTITY_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `barrel` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C7 processing/automation; C6 contents |
| `bottle_rack` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C9 display/props/rack; C32 exact display drops |
| `distillery` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C7 processing/automation; C6 contents |
| `drying_table` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C4 drying/sided automation |
| `flask` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C7 processing/automation; C6 contents |
| `fluid_attributes` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C12 source/flow metadata |
| `juniper_hanging_sign` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C11 native hanging sign |
| `juniper_sign` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C11 native sign |
| `mash_tub` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C8 batches/walls/automation |
| `mash_tub_edge` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C8 batches/walls/automation |
| `placed_drink` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C9 display/props/rack; C32 exact display drops |
| `rift_jar` | Native `BlockEntityType` via [PSBlockEntities](../src/main/java/ivorius/psychedelicraft/block/entity/PSBlockEntities.java) | C14 rifts/jars/glitch; C36 combined native rendering/reload/resize |

## PARTICLE_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `bubble` | [PSParticles$2](../src/main/java/ivorius/psychedelicraft/particle/PSParticles.java) | C5 smoke; C13 bubbles; C24 native audio/particle paths |
| `exhaled_smoke` | [PSParticles$1](../src/main/java/ivorius/psychedelicraft/particle/PSParticles.java) | C5 smoke; C13 bubbles; C24 native audio/particle paths |

## MENU_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `drying_table` | Native `MenuType` via [PSMenus](../src/main/java/ivorius/psychedelicraft/screen/PSMenus.java) | C4/C7 native menus, transfer and automation |
| `fluid_machine` | Native `MenuType` via [PSMenus](../src/main/java/ivorius/psychedelicraft/screen/PSMenus.java) | C4/C7 native menus, transfer and automation |

## RECIPE_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `drying` | [PSRecipes$1](../src/main/java/ivorius/psychedelicraft/recipe/PSRecipes.java) | C4 drying / C8 mashing native processing |
| `mashing` | [PSRecipes$2](../src/main/java/ivorius/psychedelicraft/recipe/PSRecipes.java) | C4 drying / C8 mashing native processing |

## RECIPE_SERIALIZERS

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `change_receptical` | [ChangeReceptacleRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/ChangeReceptacleRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `crafting_shaped` | [BottleRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/BottleRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `distillery` | [DistilleryRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/DistilleryRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `drying` | [DryingRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/DryingRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `dye_harmonium` | Native `SimpleCraftingRecipeSerializer` via [PSRecipes](../src/main/java/ivorius/psychedelicraft/recipe/PSRecipes.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `fill_receptical` | [FillReceptacleRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/FillReceptacleRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `mashing` | [MashingRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/MashingRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `pour_drink` | Native `SimpleCraftingRecipeSerializer` via [PSRecipes](../src/main/java/ivorius/psychedelicraft/recipe/PSRecipes.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `shapeless_fluid` | [FluidAwareShapelessRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/FluidAwareShapelessRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |
| `smelting_receptical` | [HeatingRecipe$Serializer](../src/main/java/ivorius/psychedelicraft/recipe/HeatingRecipe.java) | C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI |

## VILLAGER_PROFESSIONS

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `drug_addict` | Native `VillagerProfession` via [PSTradeOffers](../src/main/java/ivorius/psychedelicraft/entity/PSTradeOffers.java) | C15 trades/professions; C16 work; C17/C30 native acquisition |
| `drug_dealer` | Native `VillagerProfession` via [PSTradeOffers](../src/main/java/ivorius/psychedelicraft/entity/PSTradeOffers.java) | C15 trades/professions; C16 work; C17/C30 native acquisition |

## POI_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `drug_dealer` | Native `PoiType` via [PSTradeOffers](../src/main/java/ivorius/psychedelicraft/entity/PSTradeOffers.java) | C15 drying-table job acquisition |

## FEATURES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `tilled_patch` | [TilledPatchFeature](../src/main/java/ivorius/psychedelicraft/world/gen/TilledPatchFeature.java) | C3 soil-aware natural patches; C10/C11 vine/tree generation |

## FLUID_TYPES

| ID | Chosen implementation | Evidence / current status |
| --- | --- | --- |
| `agave` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `apple` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `banana` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `bath_salts` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `belladonna_extract` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `caffeine` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `cannabis_tea` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `coca_tea` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `cocaine` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `coffee` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `corn` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `honey` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `jimsonweed_extract` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `juniper` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `kava` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `milk` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `morning_glory_extract` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `peyote_juice` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `pineapple` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `potato` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `red_grapes` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `rice` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `slurry` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `sugar_cane` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `tomato` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `wheat` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |
| `wheat_hop` | [PSFluids$Family$1](../src/main/java/ivorius/psychedelicraft/fluid/PSFluids.java) | C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration |

## Every loaded mod recipe

Runtime lookup confirms these recipe objects were actually loaded. The resource path is the chosen data implementation; the processing and conservation behavior of its serializer is covered in the linked cycle report. A loaded entry is not an assertion that every ingredient combination was crafted individually.

| ID / source data | Runtime type | Runtime serializer |
| --- | --- | --- |
| [`acacia_barrel`](../src/main/resources/data/psychedelicraft/recipes/acacia_barrel.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`belladonna_seeds`](../src/main/resources/data/psychedelicraft/recipes/belladonna_seeds.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`birch_barrel`](../src/main/resources/data/psychedelicraft/recipes/birch_barrel.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`bong`](../src/main/resources/data/psychedelicraft/recipes/bong.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`bottle`](../src/main/resources/data/psychedelicraft/recipes/bottle.json) | `minecraft:crafting` | `psychedelicraft:crafting_shaped` |
| [`bottle_rack`](../src/main/resources/data/psychedelicraft/recipes/bottle_rack.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`bottle_to_molotov`](../src/main/resources/data/psychedelicraft/recipes/bottle_to_molotov.json) | `minecraft:crafting` | `psychedelicraft:change_receptical` |
| [`charcoal_from_juniper`](../src/main/resources/data/psychedelicraft/recipes/charcoal_from_juniper.json) | `minecraft:smelting` | `minecraft:smelting` |
| [`cigar`](../src/main/resources/data/psychedelicraft/recipes/cigar.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`cigarette`](../src/main/resources/data/psychedelicraft/recipes/cigarette.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`cocaine_powder`](../src/main/resources/data/psychedelicraft/recipes/cocaine_powder.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`coffea_cherries_to_coffee_beans`](../src/main/resources/data/psychedelicraft/recipes/coffea_cherries_to_coffee_beans.json) | `minecraft:smelting` | `minecraft:smelting` |
| [`dark_oak_barrel`](../src/main/resources/data/psychedelicraft/recipes/dark_oak_barrel.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`distillery`](../src/main/resources/data/psychedelicraft/recipes/distillery.json) | `minecraft:crafting` | `psychedelicraft:distillery` |
| [`drying_table`](../src/main/resources/data/psychedelicraft/recipes/drying_table.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`drying_table/brown_magic_mushrooms`](../src/main/resources/data/psychedelicraft/recipes/drying_table/brown_magic_mushrooms.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/coca_leaves`](../src/main/resources/data/psychedelicraft/recipes/drying_table/coca_leaves.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/dried_belladonna`](../src/main/resources/data/psychedelicraft/recipes/drying_table/dried_belladonna.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/dried_cannabis_buds`](../src/main/resources/data/psychedelicraft/recipes/drying_table/dried_cannabis_buds.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/dried_cannabis_leaf`](../src/main/resources/data/psychedelicraft/recipes/drying_table/dried_cannabis_leaf.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/dried_jimsonweed`](../src/main/resources/data/psychedelicraft/recipes/drying_table/dried_jimsonweed.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/dried_peyote`](../src/main/resources/data/psychedelicraft/recipes/drying_table/dried_peyote.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/red_magic_mushrooms`](../src/main/resources/data/psychedelicraft/recipes/drying_table/red_magic_mushrooms.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`drying_table/tabacco_leaves`](../src/main/resources/data/psychedelicraft/recipes/drying_table/tabacco_leaves.json) | `psychedelicraft:drying` | `psychedelicraft:drying` |
| [`dye_harmonium`](../src/main/resources/data/psychedelicraft/recipes/dye_harmonium.json) | `minecraft:crafting` | `psychedelicraft:dye_harmonium` |
| [`flask`](../src/main/resources/data/psychedelicraft/recipes/flask.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`fluids/holder_with_agave_juice`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_agave_juice.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_bath_salts`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_bath_salts.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_belladonna_extract`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_belladonna_extract.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_caffeine`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_caffeine.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_cannabis_tea`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_cannabis_tea.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_coca_tea`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_coca_tea.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_cocaine`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_cocaine.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_coffee`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_coffee.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_jimsonweed_extract`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_jimsonweed_extract.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_morning_glory_extract`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_morning_glory_extract.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/holder_with_peyote_juice`](../src/main/resources/data/psychedelicraft/recipes/fluids/holder_with_peyote_juice.json) | `minecraft:crafting` | `psychedelicraft:fill_receptical` |
| [`fluids/mash_table_with_agave`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_agave.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_apple`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_apple.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_banana`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_banana.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_corn`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_corn.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_honey`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_honey.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_juniper`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_juniper.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_pineapple`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_pineapple.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_potato`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_potato.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_red_grapes`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_red_grapes.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_rice`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_rice.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_sugar_cane`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_sugar_cane.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_tomato`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_tomato.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_wheat`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_wheat.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/mash_table_with_wheat_hop`](../src/main/resources/data/psychedelicraft/recipes/fluids/mash_table_with_wheat_hop.json) | `psychedelicraft:mashing` | `psychedelicraft:mashing` |
| [`fluids/pour_drink`](../src/main/resources/data/psychedelicraft/recipes/fluids/pour_drink.json) | `minecraft:crafting` | `psychedelicraft:pour_drink` |
| [`glass_chalice`](../src/main/resources/data/psychedelicraft/recipes/glass_chalice.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`harmonium`](../src/main/resources/data/psychedelicraft/recipes/harmonium.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`hash_muffin`](../src/main/resources/data/psychedelicraft/recipes/hash_muffin.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`hot_coffee`](../src/main/resources/data/psychedelicraft/recipes/hot_coffee.json) | `minecraft:smelting` | `psychedelicraft:smelting_receptical` |
| [`iron_drying_table`](../src/main/resources/data/psychedelicraft/recipes/iron_drying_table.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`jimsonweed_seeds`](../src/main/resources/data/psychedelicraft/recipes/jimsonweed_seeds.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`joint`](../src/main/resources/data/psychedelicraft/recipes/joint.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`jungle_barrel`](../src/main/resources/data/psychedelicraft/recipes/jungle_barrel.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_boat`](../src/main/resources/data/psychedelicraft/recipes/juniper_boat.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_button`](../src/main/resources/data/psychedelicraft/recipes/juniper_button.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`juniper_chest_boat`](../src/main/resources/data/psychedelicraft/recipes/juniper_chest_boat.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`juniper_door`](../src/main/resources/data/psychedelicraft/recipes/juniper_door.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_fence`](../src/main/resources/data/psychedelicraft/recipes/juniper_fence.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_fence_gate`](../src/main/resources/data/psychedelicraft/recipes/juniper_fence_gate.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_hanging_sign`](../src/main/resources/data/psychedelicraft/recipes/juniper_hanging_sign.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_planks`](../src/main/resources/data/psychedelicraft/recipes/juniper_planks.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`juniper_pressure_plate`](../src/main/resources/data/psychedelicraft/recipes/juniper_pressure_plate.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_sign`](../src/main/resources/data/psychedelicraft/recipes/juniper_sign.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_slab`](../src/main/resources/data/psychedelicraft/recipes/juniper_slab.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_stairs`](../src/main/resources/data/psychedelicraft/recipes/juniper_stairs.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_trapdoor`](../src/main/resources/data/psychedelicraft/recipes/juniper_trapdoor.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`juniper_wood`](../src/main/resources/data/psychedelicraft/recipes/juniper_wood.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`lattice`](../src/main/resources/data/psychedelicraft/recipes/lattice.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`lsa_square`](../src/main/resources/data/psychedelicraft/recipes/lsa_square.json) | `minecraft:crafting` | `psychedelicraft:shapeless_fluid` |
| [`mash_tub`](../src/main/resources/data/psychedelicraft/recipes/mash_tub.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`molotov_cocktail`](../src/main/resources/data/psychedelicraft/recipes/molotov_cocktail.json) | `minecraft:crafting` | `psychedelicraft:crafting_shaped` |
| [`molotov_to_bottle`](../src/main/resources/data/psychedelicraft/recipes/molotov_to_bottle.json) | `minecraft:crafting` | `psychedelicraft:change_receptical` |
| [`morning_glory_to_seeds`](../src/main/resources/data/psychedelicraft/recipes/morning_glory_to_seeds.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`oak_barrel`](../src/main/resources/data/psychedelicraft/recipes/oak_barrel.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`obsidian_bottle`](../src/main/resources/data/psychedelicraft/recipes/obsidian_bottle.json) | `minecraft:crafting` | `psychedelicraft:shapeless_fluid` |
| [`obsidian_dust`](../src/main/resources/data/psychedelicraft/recipes/obsidian_dust.json) | `minecraft:crafting` | `psychedelicraft:shapeless_fluid` |
| [`obsidian_dust_to_obsidian`](../src/main/resources/data/psychedelicraft/recipes/obsidian_dust_to_obsidian.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`paper_bag`](../src/main/resources/data/psychedelicraft/recipes/paper_bag.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`peyote_joint`](../src/main/resources/data/psychedelicraft/recipes/peyote_joint.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`pipe`](../src/main/resources/data/psychedelicraft/recipes/pipe.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`rift_jar`](../src/main/resources/data/psychedelicraft/recipes/rift_jar.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`shot_glass`](../src/main/resources/data/psychedelicraft/recipes/shot_glass.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`spruce_barrel`](../src/main/resources/data/psychedelicraft/recipes/spruce_barrel.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`stone_cup`](../src/main/resources/data/psychedelicraft/recipes/stone_cup.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`syringe`](../src/main/resources/data/psychedelicraft/recipes/syringe.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |
| [`tomato_seeds`](../src/main/resources/data/psychedelicraft/recipes/tomato_seeds.json) | `minecraft:crafting` | `minecraft:crafting_shapeless` |
| [`wooden_mug`](../src/main/resources/data/psychedelicraft/recipes/wooden_mug.json) | `minecraft:crafting` | `minecraft:crafting_shaped` |

## Every advancement

All 24 use the restored native criteria and client award UI (C27). C32 separately verifies the actual native trident/Channeling awards.

| ID / data source | Evidence |
| --- | --- |
| [`all_drugs`](../src/main/resources/data/psychedelicraft/advancements/all_drugs.json) | C27 criteria, native awards, localization and restart |
| [`cannabis_buds`](../src/main/resources/data/psychedelicraft/advancements/cannabis_buds.json) | C27 criteria, native awards, localization and restart |
| [`drink_basi`](../src/main/resources/data/psychedelicraft/advancements/drink_basi.json) | C27 criteria, native awards, localization and restart |
| [`drink_beer`](../src/main/resources/data/psychedelicraft/advancements/drink_beer.json) | C27 criteria, native awards, localization and restart |
| [`drink_brandy`](../src/main/resources/data/psychedelicraft/advancements/drink_brandy.json) | C27 criteria, native awards, localization and restart |
| [`drink_mature_beer`](../src/main/resources/data/psychedelicraft/advancements/drink_mature_beer.json) | C27 criteria, native awards, localization and restart |
| [`drink_rum`](../src/main/resources/data/psychedelicraft/advancements/drink_rum.json) | C27 criteria, native awards, localization and restart |
| [`drink_wine`](../src/main/resources/data/psychedelicraft/advancements/drink_wine.json) | C27 criteria, native awards, localization and restart |
| [`dry_brown_mushroom`](../src/main/resources/data/psychedelicraft/advancements/dry_brown_mushroom.json) | C27 criteria, native awards, localization and restart |
| [`dry_cannabis_buds`](../src/main/resources/data/psychedelicraft/advancements/dry_cannabis_buds.json) | C27 criteria, native awards, localization and restart |
| [`dry_red_mushroom`](../src/main/resources/data/psychedelicraft/advancements/dry_red_mushroom.json) | C27 criteria, native awards, localization and restart |
| [`feed_a_villager`](../src/main/resources/data/psychedelicraft/advancements/feed_a_villager.json) | C27 criteria, native awards, localization and restart |
| [`grapes`](../src/main/resources/data/psychedelicraft/advancements/grapes.json) | C27 criteria, native awards, localization and restart |
| [`hangover`](../src/main/resources/data/psychedelicraft/advancements/hangover.json) | C27 criteria, native awards, localization and restart |
| [`hash_a_muffin`](../src/main/resources/data/psychedelicraft/advancements/hash_a_muffin.json) | C27 criteria, native awards, localization and restart |
| [`hop_cones`](../src/main/resources/data/psychedelicraft/advancements/hop_cones.json) | C27 criteria, native awards, localization and restart |
| [`make_distillery`](../src/main/resources/data/psychedelicraft/advancements/make_distillery.json) | C27 criteria, native awards, localization and restart |
| [`make_drying_table`](../src/main/resources/data/psychedelicraft/advancements/make_drying_table.json) | C27 criteria, native awards, localization and restart |
| [`make_mash_tub`](../src/main/resources/data/psychedelicraft/advancements/make_mash_tub.json) | C27 criteria, native awards, localization and restart |
| [`roll_joint`](../src/main/resources/data/psychedelicraft/advancements/roll_joint.json) | C27 criteria, native awards, localization and restart |
| [`root`](../src/main/resources/data/psychedelicraft/advancements/root.json) | C27 criteria, native awards, localization and restart |
| [`wash_beer`](../src/main/resources/data/psychedelicraft/advancements/wash_beer.json) | C27 criteria, native awards, localization and restart |
| [`wash_grapes`](../src/main/resources/data/psychedelicraft/advancements/wash_grapes.json) | C27 criteria, native awards, localization and restart |
| [`wash_sugar_cane`](../src/main/resources/data/psychedelicraft/advancements/wash_sugar_cane.json) | C27 criteria, native awards, localization and restart |

## Every drug profile

These are the mod's scalar profiles, not registered vanilla potion effects. The accepted `coccaine` spelling is retained for saved/command identity. C2 covers the scalar/use hooks; C19–C24 and C29 cover the restored audiovisual/text channels. C36 verifies combined rendering. C37 verifies a seven-profile custom-benefit cohort across its installed multiplayer lifecycle.

| ID | Chosen implementation | Status |
| --- | --- | --- |
| `brown_shrooms` | [BrownShroomsDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/BrownShroomsDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `red_shrooms` | [RedShroomsDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/RedShroomsDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `peyote` | [PeyoteDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/PeyoteDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `atropine` | [LsdDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/LsdDrug.java) | C2 scalar profile and shared progression |
| `caffeine` | [CaffeineDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/CaffeineDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `warmth` | [WarmthDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/WarmthDrug.java) | C2 scalar profile and shared progression |
| `alcohol` | [AlcoholDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/AlcoholDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `kava` | [AlcoholDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/AlcoholDrug.java) | C2 scalar profile and shared progression |
| `cannabis` | [CannabisDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/CannabisDrug.java) | C2 scalar profile and shared progression |
| `tobacco` | [TobaccoDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/TobaccoDrug.java) | C2 scalar profile and shared progression, C5 smoking and C37 native saturation |
| `coccaine` | [CocaineDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/CocaineDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `bath_salts` | [BathSaltsDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/BathSaltsDrug.java) | C2 scalar profile and shared progression |
| `lsd` | [LsdDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/LsdDrug.java) | C2 scalar profile and shared progression, C37 installed benefit and lifecycle checks |
| `sugar` | [CaffeineDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/CaffeineDrug.java) | C2 scalar profile and shared progression |
| `harmonium` | [HarmoniumDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/HarmoniumDrug.java) | C2 scalar profile and shared progression |
| `zero` | [SimpleDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/SimpleDrug.java) | C2 scalar profile and shared progression |
| `power` | [PowerDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/PowerDrug.java) | C2 scalar profile and shared progression |
| `sleep_deprivation` | [SleepDeprivationDrug](../src/main/java/ivorius/psychedelicraft/entity/drug/type/SleepDeprivationDrug.java) | C2 scalar profile and shared progression |

## Every setting and criterion

The live config-spec export contains **106 common** and **34 client** leaves. Each is listed with its accepted source, default, consumer and evidence in [the settings matrix](PORT_1201_SETTINGS.md). Its native UI/EN/PL/scope/persistence checks are in C31; those source files are unchanged through C35. The separate native gamerule `doSleepDeprivation` is registered in `Psychedelicraft` and covered in C2/C22/C27.

| Native criterion ID | Chosen implementation / evidence |
| --- | --- |
| `custom` | [PSCriteria / DrugEffectsChangedCriterion](../src/main/java/ivorius/psychedelicraft/advancement/PSCriteria.java); C27 native award/clear/restart |
| `drank_brew` | [PSCriteria / DrugEffectsChangedCriterion](../src/main/java/ivorius/psychedelicraft/advancement/PSCriteria.java); C27 native award/clear/restart |
| `drug_effects_changed` | [PSCriteria / DrugEffectsChangedCriterion](../src/main/java/ivorius/psychedelicraft/advancement/PSCriteria.java); C27 native award/clear/restart |
| `mashed_item` | [PSCriteria / DrugEffectsChangedCriterion](../src/main/java/ivorius/psychedelicraft/advancement/PSCriteria.java); C27 native award/clear/restart |

## Every operator command

Actual Brigadier usage, obtained from the running server with its operator source. C18/C23/C29 cover execution, permissions, synchronization and parsing; C35 records the current tree.

| Native root / usage | Source |
| --- | --- |
| `/drug <target> get` | [command/DrugCommand](../src/main/java/ivorius/psychedelicraft/command/DrugCommand.java) |
| `/drug <target> get <drug>` | [command/DrugCommand](../src/main/java/ivorius/psychedelicraft/command/DrugCommand.java) |
| `/drug <target> set <drug> <value>` | [command/DrugCommand](../src/main/java/ivorius/psychedelicraft/command/DrugCommand.java) |
| `/drug <target> add <drug> <value>` | [command/DrugCommand](../src/main/java/ivorius/psychedelicraft/command/DrugCommand.java) |
| `/drug <target> lock <drug> <locked>` | [command/DrugCommand](../src/main/java/ivorius/psychedelicraft/command/DrugCommand.java) |
| `/vomit` | [command/VomitCommand](../src/main/java/ivorius/psychedelicraft/command/VomitCommand.java) |
| `/vomit <target>` | [command/VomitCommand](../src/main/java/ivorius/psychedelicraft/command/VomitCommand.java) |
| `/hallucinate <type>` | [command/HallucinateCommand](../src/main/java/ivorius/psychedelicraft/command/HallucinateCommand.java) |
| `/hallucinate <type> <position>` | [command/HallucinateCommand](../src/main/java/ivorius/psychedelicraft/command/HallucinateCommand.java) |
| `/hallucinate <type> <targets>` | [command/HallucinateCommand](../src/main/java/ivorius/psychedelicraft/command/HallucinateCommand.java) |
| `/hallucinate <type> <targets> <position>` | [command/HallucinateCommand](../src/main/java/ivorius/psychedelicraft/command/HallucinateCommand.java) |

## Additional dynamic registrations

These IDs are loaded from the server's registry access. Native registry duplicates of the tables above are omitted here. World-generation sampling and configuration are covered in C3/C10/C11, native pool graphs in C30, and custom damage/lifecycle behavior in C2/C13/C18/C24.

| Registry | ID | Chosen implementation / evidence |
| --- | --- | --- |
| `minecraft:creative_mode_tab` | `main` | item/PSItems.java; C25/C31 native creative/search rebuilding |
| `minecraft:damage_type` | `alcohol_poisoning` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `heart_attack` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `heart_failure` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `in_sleep` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `kidney_failure` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `molotov` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `over_eating` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `overdose` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `respiratory_failure` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `self_molotov` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:damage_type` | `stroke` | PSDamageTypes.java + damage_type data; C2/C13/C18/C24 |
| `minecraft:worldgen/configured_feature` | `agave_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `belladonna_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `cannabis_tilled_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `coca_jungle_tilled_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `coca_tilled_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `coffea_tilled_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `hop_tilled_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `jimsonweed_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `juniper_tree` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `morning_glory_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `peyote_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `tobacco_tilled_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/configured_feature` | `tomato_patch` | world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `agave_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `agave_patch_unchecked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `belladonna_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `belladonna_patch_unchecked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `cannabis_tilled_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `coca_jungle_tilled_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `coca_tilled_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `coffea_tilled_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `hop_tilled_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `jimsonweed_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `jimsonweed_patch_unchecked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `juniper_tree_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `morning_glory_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `morning_glory_patch_unchecked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `peyote_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `peyote_patch_unchecked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `tobacco_tilled_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `tomato_patch_checked` | worldgen/placed_feature data; C3/C10/C11 |
| `minecraft:worldgen/placed_feature` | `tomato_patch_unchecked` | worldgen/placed_feature data; C3/C10/C11 |
| `forge:biome_modifier` | `agave` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `belladonna` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `cannabis` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `coca` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `coca_jungle` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `coffea` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `hop` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `jimsonweed` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `juniper` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `morning_glory` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `peyote` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `tobacco` | forge/biome_modifier data; C3/C10/C11 |
| `forge:biome_modifier` | `tomato` | forge/biome_modifier data; C3/C10/C11 |
| `forge:global_loot_modifier_serializers` | `chest_supplies` | world/gen/ChestLootModifier.java; C17 acquisition loot |

## Accepted and original registry reconciliation

This source comparison strips Java comments before extracting literal factory declarations, then explicitly expands the accepted six `barrel(PSBlocks.*)` items and the original Terraform boat helper's two items. It is a comparison of these pinned sources, not a generic Java evaluator. The runtime tables above remain the authority for what actually loaded.

### Items

Accepted named entries: **144**. Original named entries including the explicit helper expansion: **100**. Original entries absent from the runtime mod registry: **none**.

| Accepted ID absent from mod namespace | Native target / state mapping | Evidence / scope |
| --- | --- | --- |
| `angler_pottery_sherd` | `minecraft:angler_pottery_sherd` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `azalea` | `minecraft:azalea` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `azalea_leaves` | `minecraft:azalea_leaves` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `bee_nest` | `minecraft:bee_nest` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `brush` | `minecraft:brush` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `campfire` | `minecraft:campfire` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `conduit` | `minecraft:conduit` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `copper_ingot` | `minecraft:copper_ingot` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `copper_ore` | `minecraft:copper_ore` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `cornflower` | `minecraft:cornflower` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `dried_kelp` | `minecraft:dried_kelp` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `dried_kelp_block` | `minecraft:dried_kelp_block` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `flowering_azalea` | `minecraft:flowering_azalea` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `flowering_azalea_leaves` | `minecraft:flowering_azalea_leaves` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `glowing_ink` | `minecraft:glow_ink_sac` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `hanging_roots` | `minecraft:hanging_roots` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `heart_of_the_sea` | `minecraft:heart_of_the_sea` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `honey_bottle` | `minecraft:honey_bottle` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `honeycomb` | `minecraft:honeycomb` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `iron_chain` | `minecraft:chain` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `kelp` | `minecraft:kelp` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `lily_of_the_valley` | `minecraft:lily_of_the_valley` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `moss_block` | `minecraft:moss_block` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `moss_carpet` | `minecraft:moss_carpet` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `mud` | `minecraft:mud` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `nautilus_shell` | `minecraft:nautilus_shell` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `pitcher_plant` | `minecraft:pitcher_plant` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `pitcher_pod` | `minecraft:pitcher_pod` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `raw_copper` | `minecraft:raw_copper` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `rooted_dirt` | `minecraft:rooted_dirt` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `shelter_pottery_sherd` | `minecraft:shelter_pottery_sherd` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `sign_black_dye` | `minecraft:black_dye` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `sign_wax` | `minecraft:honeycomb` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `sniffer_egg` | `minecraft:sniffer_egg` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `snort_pottery_sherd` | `minecraft:snort_pottery_sherd` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `soul_campfire` | `minecraft:soul_campfire` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `soul_soil` | `minecraft:soul_soil` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `suspicious_sand` | `minecraft:suspicious_sand` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `suspicious_stew` | `minecraft:suspicious_stew` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `sweet_berries` | `minecraft:sweet_berries` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `torchflower` | `minecraft:torchflower` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `torchflower_seeds` | `minecraft:torchflower_seeds` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `trident` | `minecraft:trident` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `wither_rose` | `minecraft:wither_rose` | Native implementation; interaction evidence and remaining scope in native mapping report |

Target additions relative to the original named table: none.

### Blocks

Accepted named entries: **89**. Original named entries including the explicit helper expansion: **59**. Original entries absent from the runtime mod registry: **none**.

| Accepted ID absent from mod namespace | Native target / state mapping | Evidence / scope |
| --- | --- | --- |
| `azalea` | `minecraft:azalea` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `azalea_leaves` | `minecraft:azalea_leaves` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `bee_nest` | `minecraft:bee_nest` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `bubble_column` | `minecraft:bubble_column` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `campfire` | `minecraft:campfire` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `conduit` | `minecraft:conduit` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `copper_ore` | `minecraft:copper_ore` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `cornflower` | `minecraft:cornflower` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `dried_kelp_block` | `minecraft:dried_kelp_block` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `flowering_azalea` | `minecraft:flowering_azalea` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `flowering_azalea_leaves` | `minecraft:flowering_azalea_leaves` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `hanging_roots` | `minecraft:hanging_roots` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `juniper_double_slab` | `psychedelicraft:juniper_slab[type=double]` | C11 native slab state |
| `kelp` | `minecraft:kelp` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `kelp_stalk` | `minecraft:kelp_plant` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `lily_of_the_valley` | `minecraft:lily_of_the_valley` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `moss_block` | `minecraft:moss_block` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `moss_carpet` | `minecraft:moss_carpet` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `mud` | `minecraft:mud` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `natural_cannabis` | `psychedelicraft:cannabis[natural=true]` | C3 native crop state replaces duplicate wild block |
| `natural_coca` | `psychedelicraft:coca[natural=true]` | C3 native crop state replaces duplicate wild block |
| `natural_coffea` | `psychedelicraft:coffea[natural=true]` | C3 native crop state replaces duplicate wild block |
| `natural_hop` | `psychedelicraft:hop[natural=true]` | C3 native crop state replaces duplicate wild block |
| `natural_tobacco` | `psychedelicraft:tobacco[natural=true]` | C3 native crop state replaces duplicate wild block |
| `pitcher_crop` | `minecraft:pitcher_crop` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `pitcher_plant` | `minecraft:pitcher_plant` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `powered_juniper_trapdoor` | `psychedelicraft:juniper_trapdoor[powered=true]` | C11 native redstone state |
| `rooted_dirt` | `minecraft:rooted_dirt` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `sniffer_egg` | `minecraft:sniffer_egg` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `soul_campfire` | `minecraft:soul_campfire` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `soul_fire` | `minecraft:soul_fire` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `soul_soil` | `minecraft:soul_soil` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `suspicious_sand` | `minecraft:suspicious_sand` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `sweet_berry_bush` | `minecraft:sweet_berry_bush` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `torchflower` | `minecraft:torchflower` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `torchflower_crop` | `minecraft:torchflower_crop` | Native implementation; interaction evidence and remaining scope in native mapping report |
| `wither_rose` | `minecraft:wither_rose` | Native implementation; interaction evidence and remaining scope in native mapping report |

Target additions relative to the original named table: `agave`, `apple`, `banana`, `bath_salts`, `belladonna_extract`, `caffeine`, `cannabis_tea`, `coca_tea`, `cocaine`, `coffee`, `corn`, `honey`, `jimsonweed_extract`, `juniper`, `kava`, `milk`, `morning_glory_extract`, `peyote_juice`, `pineapple`, `potato`, `red_grapes`, `rice`, `slurry`, `sugar_cane`, `tomato`, `wheat`, `wheat_hop`.

The original `kava_seeds` and `kava_root` lines are commented TODOs, not registered items. The 27 additional target block IDs correspond to the original dynamically registered physical-fluid families, not additional gameplay. The seven native potted crop/sapling forms are included in the original block table and all are present. [The native mapping report](PORT_1201_NATIVE.md) records the deliberate namespace migration for new worlds; no legacy-world importer is claimed.

### Fluid families

Accepted source families: **27**; original: **27**; runtime: **27**. Accepted families absent at runtime: **none**. Original families absent at runtime: **none**. Each runtime source has its explicitly listed flowing registration, native liquid block and Forge fluid type. The original physical-fluid registration is dynamic; C12 verifies the actual source/flow/block/metadata paths.

### Entity types

All accepted entity registrations are accounted for. The four mod-specific types remain mod types; the six vanilla replacements use native entities. C35 binds the four current mod renderers. The native report retains the model/acquisition/lifecycle scopes not yet observed.

| Accepted ID | Target entity ID | Evidence / scope |
| --- | --- | --- |
| `bee` | `minecraft:bee` | Native implementation; see native mapping report for bounded evidence |
| `drowned` | `minecraft:drowned` | Native implementation; see native mapping report for bounded evidence |
| `juniper_boat` | `psychedelicraft:juniper_boat` | C11 wood/fruit/vehicle integration |
| `juniper_chest_boat` | `psychedelicraft:juniper_chest_boat` | C11 wood/fruit/vehicle integration |
| `molotov_cocktail` | `psychedelicraft:molotov_cocktail` | C13 native projectile and fluid-preserving conversion |
| `reality_rift` | `psychedelicraft:reality_rift` | C14 rifts/jars/glitch; C36 combined native rendering/reload/resize |
| `sniffer` | `minecraft:sniffer` | Native implementation; see native mapping report for bounded evidence |
| `thrown_trident` | `minecraft:trident` | Native implementation; see native mapping report for bounded evidence |
| `trader_llama` | `minecraft:trader_llama` | Native implementation; see native mapping report for bounded evidence |
| `wandering_trader` | `minecraft:wandering_trader` | Native implementation; see native mapping report for bounded evidence |

### Block entity types and native wood helpers

| Accepted block entity ID | Target / state mapping |
| --- | --- |
| `fluid_attributes` | `psychedelicraft:fluid_attributes` |
| `brushable_block` | `minecraft:brushable_block` |
| `drying_table` | `psychedelicraft:drying_table` |
| `mash_tub` | `psychedelicraft:mash_tub` |
| `mash_tub_edge` | `psychedelicraft:mash_tub_edge` |
| `barrel` | `psychedelicraft:barrel` |
| `flask` | `psychedelicraft:flask` |
| `distillery` | `psychedelicraft:distillery` |
| `placed_drink` | `psychedelicraft:placed_drink` |
| `bottle_rack` | `psychedelicraft:bottle_rack` |
| `lattice_crop` | `lattice block states replace the legacy tile` |
| `waterlogged_block` | `native WATERLOGGED state/interfaces` |
| `peyote` | `native block AGE state` |
| `juniper_sign` | `psychedelicraft:juniper_sign` |
| `rift_jar` | `psychedelicraft:rift_jar` |
| `bee_nest` | `minecraft:beehive` |
| `campfire` | `minecraft:campfire` |
| `conduit` | `minecraft:conduit` |

The target adds `juniper_hanging_sign` as the native separate hanging-sign block-entity type. `PSWoodTypes` registers `psychedelicraft:juniper` once in each native WoodType and BlockSetType helper registry; C11 verifies their native wood/sign behavior.

## Observation limits and remaining work

C35 inspects **1509** MODEL block states, 100 native default item renders per language, all 4 mod entity-renderer bindings, loaded recipes/advancements and all 18 custom profiles. Each mod item-description key is also checked directly in both packaged language catalogs, independently of native fallback resolution. Quad/sprite observations use seed zero for each direction and the unculled face; this does not enumerate every weighted-model outcome. The production archive contains **8** sound definitions; local sound references with missing/invalid Ogg headers: **0**. Native/delegated sound events and dynamic music have separate C22/C24 runtime evidence.

The complete 140-setting source/consumer/default/evidence matrix remains in [PORT_1201_SETTINGS.md](PORT_1201_SETTINGS.md), with native UI/EN/PL/save/restart/scope evidence in C31. This inventory does not turn a registered ID or a valid model into broad gameplay proof. Read the later actual behavior samples in the cycle report. C37 verifies death, respawn, dimension travel, custom benefits and near-end expiry. C38 and C39 verify native ocean and wild crop acquisition.

Audit findings needing review:

- None in the covered registry/model/default-name/local-audio checks.
