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
