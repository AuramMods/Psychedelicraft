package ivorius.psychedelicraft.world.gen;

import java.util.function.Predicate;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

final class PlantBiomes {
    private PlantBiomes() {
    }

    static boolean allows(PSConfig.Feature options, Predicate<Biome> defaults, Biome biome) {
        if (!options.enabled) {
            return false;
        }
        for (String selector : options.excluded) {
            if (matches(selector, biome)) {
                return false;
            }
        }
        if (defaults.test(biome)) {
            return true;
        }
        if (options.included.length == 0) {
            return false;
        }
        for (String selector : options.included) {
            if (!matches(selector, biome)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(String selector, Biome biome) {
        String value = selector.trim();
        if (!value.startsWith("#")) {
            return !value.isEmpty() && new ResourceLocation(value).equals(biome.getRegistryName());
        }
        ResourceLocation tag = new ResourceLocation(value.substring(1));
        if ("forge".equals(tag.getResourceDomain())) {
            for (BiomeDictionary.Type type : BiomeDictionary.getTypes(biome)) {
                if (type.getName().equalsIgnoreCase(tag.getResourcePath())) {
                    return true;
                }
            }
        } else if ("minecraft".equals(tag.getResourceDomain())) {
            switch (tag.getResourcePath()) {
                case "is_forest": return forest(biome);
                case "is_hill": return hills(biome);
                case "is_jungle": return jungle(biome);
                case "is_savanna": return savanna(biome);
                case "is_badlands": return badlands(biome);
                case "has_structure/desert_pyramid": return desert(biome);
                default: break;
            }
        }
        return false;
    }

    static boolean tallCrops(Biome biome) {
        return biome == Biomes.PLAINS || biome.getDefaultTemperature() < 0.15F || hills(biome) || forest(biome);
    }

    static boolean morningGlory(Biome biome) {
        return biome == Biomes.MUTATED_FOREST || biome == Biomes.MUTATED_PLAINS;
    }

    static boolean belladonna(Biome biome) {
        return biome == Biomes.ROOFED_FOREST || biome == Biomes.MUTATED_ROOFED_FOREST;
    }

    static boolean jungle(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE);
    }

    static boolean tomato(Biome biome) {
        return biome == Biomes.FOREST || biome == Biomes.FOREST_HILLS;
    }

    static boolean peyote(Biome biome) {
        return savanna(biome) || agave(biome);
    }

    static boolean agave(Biome biome) {
        return badlands(biome) || desert(biome) || (!biome.canRain() && !biome.isSnowyBiome());
    }

    private static boolean forest(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS) && !jungle(biome);
    }

    private static boolean hills(Biome biome) {
        return biome == Biomes.EXTREME_HILLS || biome == Biomes.EXTREME_HILLS_EDGE
                || biome == Biomes.EXTREME_HILLS_WITH_TREES || biome == Biomes.MUTATED_EXTREME_HILLS
                || biome == Biomes.MUTATED_EXTREME_HILLS_WITH_TREES;
    }

    private static boolean savanna(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.SAVANNA);
    }

    private static boolean badlands(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA);
    }

    private static boolean desert(Biome biome) {
        return biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS || biome == Biomes.MUTATED_DESERT;
    }
}
