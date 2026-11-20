# Psychedelicraft Guide (Forge 1.20.1)

## Artwork

The guide reuses the 16×16 inventory book sprite and 512×256 Patchouli GUI atlas
from [Satiscraftory](https://github.com/AuramMods/Satiscraftory) under
`assets/satiscraftory/textures/item/book.png` and `assets/satiscraftory/textures/gui/book.png`.
The recolor follows the approach documented in AuramCore's `docs/guide-artwork.json`.
A per-pixel HSV adjustment maps saturated gold and orange to crimson, keeping
the pale parchment and neutral icons. Both textures retain the original
dimensions, sprite positions, and pixel-for-pixel alpha.

## Coverage

The [Forge registry inventory](PORT_1201_REGISTRY.md), [native behavior notes](PORT_1201_NATIVE.md),
and [settings matrix](PORT_1201_SETTINGS.md) identify the systems covered by the guide.
The guide presents gameplay families together where variants share the same controls.

| Guide section | Systems covered |
| --- | --- |
| Getting Started | Manual guide crafting and a first survival harvest |
| Finding & Growing | All twelve wild plant/tree targets, vineyard grapes, both vanilla mushroom colors, seed and soil requirements, tall stalks, shears, vine supports, and world generation settings |
| Tools & Automation | Both drying tables and all nine drying inputs, mash tub, flask, six barrel variants, distillery, slurry, portable drink and syringe capacities, fluid transfer, and sided automation |
| Drinks & Preparations | All thirteen mash recipes, all fifteen brew profiles, the twelve other fluid families, tea, coffee roasting and heating, flower concentrates, LSA, smoking, prepared mushrooms, and obsidian |
| Effects & Recovery | `brown_shrooms`, `red_shrooms`, `peyote`, `atropine`, `caffeine`, `warmth`, `alcohol`, `kava`, `cannabis`, `tobacco`, `coccaine`, `bath_salts`, `lsd`, `sugar`, and `sleep_deprivation` |
| Beyond the Workshop | Village jobs and offers, seven loot modifiers, the advancement branches, displays and rack, paper bag and stomach, local and server settings, world fluids and conduits, Molotov items, and the three operator command roots |
| Optional Discoveries | `harmonium`, `zero`, and `power` profiles, reality rifts and jars, Glitch, contextual hallucinations, and Reggie |
| Troubleshooting | Plant placement, crop growth, missing trades, automation, heat, stalled batches, vessel compatibility, and effect settings |

Gameplay sources are the Forge 1.20.1 code and datapack resources. Optional pack items
use the current ingredient tags. Settings and numeric examples describe the stock port.

## Verification

- The guide ships as 8 categories, 67 entries and 211 pages. The resource audit
  resolved 49 internal links and all category, icon, item and recipe IDs.
- The reobfuscated jar ran on dedicated Forge 47.4.28 with Patchouli 84.1.
  At GUI scales 1 and 2, the isolated clients opened all 131 guide screens,
  laid out all 211 pages and resolved all 36 live crafting or smelting diagrams.
  A native click opened the linked coffea entry. The smallest text layout scale
  was 0.98. Using Patchouli's block font keeps the body text legible at GUI scale 1.
- A new survival player had an empty inventory on login and after a real respawn.
  Native inventory clicks consumed a book and red dye and produced exactly
  one guide with book ID `psychedelicraft:psychedelicraft`. Using the held item
  opened the crimson guide. The build contains only the conditional crafting
  recipe as an acquisition path.
- A separate client and dedicated server reached the world without Patchouli.
  Neither side had the guide recipe. All three dedicated runs saved and stopped
  cleanly, and the three clients exited zero. The clients used muted Xvfb
  displays and separate test directories.
- The Java 17 build passed 94 unit tests and all 303 required Forge GameTests.
  The packaged jar matched every guide resource in the source tree.

The test logs, screenshot gallery and per-run results are stored locally under
`run/production/patchouli-guide/`. The original guide verification used
`build/libs/psychedelicraft-1.20.1-port.1.jar` with SHA-256
`09b20b4863a10e89d6797c44179b2278e8dd72881b75082b53fac231d83444d7`.
