package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlantBiomesTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void biomeDefaultsUseTheirLegacyCounterparts() {
        assertTrue(PlantBiomes.tallCrops(Biomes.PLAINS));
        assertTrue(PlantBiomes.tallCrops(Biomes.EXTREME_HILLS));
        assertTrue(PlantBiomes.tallCrops(Biomes.BIRCH_FOREST));
        assertFalse(PlantBiomes.tallCrops(Biomes.DESERT_HILLS));
        assertTrue(PlantBiomes.morningGlory(Biomes.MUTATED_FOREST));
        assertTrue(PlantBiomes.morningGlory(Biomes.MUTATED_PLAINS));
        assertTrue(PlantBiomes.peyote(Biomes.SAVANNA));
        assertTrue(PlantBiomes.agave(Biomes.MESA));
        assertFalse(PlantBiomes.agave(Biomes.ICE_PLAINS));
    }

    @Test
    public void namedCropDefaultsIncludeTheLegacyBiomesMergedIntoTheirModernNames() {
        assertTrue(PlantBiomes.belladonna(Biomes.ROOFED_FOREST));
        assertTrue(PlantBiomes.belladonna(Biomes.MUTATED_ROOFED_FOREST));
        assertFalse(PlantBiomes.belladonna(Biomes.MUTATED_FOREST));
        assertTrue(PlantBiomes.jungle(Biomes.JUNGLE));
        assertTrue(PlantBiomes.jungle(Biomes.JUNGLE_HILLS));
        assertTrue(PlantBiomes.jungle(Biomes.MUTATED_JUNGLE));
        assertTrue(PlantBiomes.jungle(Biomes.JUNGLE_EDGE));
        assertTrue(PlantBiomes.jungle(Biomes.MUTATED_JUNGLE_EDGE));
        assertFalse(PlantBiomes.jungle(Biomes.PLAINS));
        assertTrue(PlantBiomes.tomato(Biomes.FOREST));
        assertTrue(PlantBiomes.tomato(Biomes.FOREST_HILLS));
        assertFalse(PlantBiomes.tomato(Biomes.MUTATED_FOREST));
        assertFalse(PlantBiomes.tomato(Biomes.BIRCH_FOREST));
    }

    @Test
    public void exclusionsWinOverDefaultsAndInclusion() {
        PSConfig.Feature options = new PSConfig.Feature();
        options.included = new String[]{"minecraft:forest"};
        options.excluded = new String[]{"#minecraft:is_forest"};
        assertFalse(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.FOREST));
        assertTrue(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.PLAINS));
        options.enabled = false;
        assertFalse(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.PLAINS));
    }

    @Test
    public void cocaRecognizesModdedJunglesWithoutChangingVanillaBiomeSelectors() {
        Biome miniJungle = new Biome(new Biome.BiomeProperties("Test Mini Jungle")) {};
        miniJungle.setRegistryName("psychedelicraft", "test_mini_jungle");
        ForgeRegistries.BIOMES.register(miniJungle);
        BiomeDictionary.addTypes(miniJungle, BiomeDictionary.Type.JUNGLE);
        PSConfig.Feature options = new PSConfig.Feature();
        assertTrue(PlantBiomes.cocaJungle(Biomes.JUNGLE_HILLS));
        assertTrue(PlantBiomes.allows(options, PlantBiomes::cocaJungle, miniJungle));
        assertFalse(PlantBiomes.cocaJungle(Biomes.RIVER));
        assertFalse(PlantBiomes.cocaJungle(Biomes.BEACH));
        assertFalse(PlantBiomes.jungle(miniJungle));
        options.excluded = new String[]{"#forge:JUNGLE"};
        assertFalse(PlantBiomes.allows(options, PlantBiomes::cocaJungle, miniJungle));
    }

    @Test
    public void allInclusionSelectorsMustMatchToExtendDefaults() {
        PSConfig.Feature options = new PSConfig.Feature();
        options.included = new String[]{"minecraft:desert", "#forge:SANDY"};
        assertTrue(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.DESERT));
        assertFalse(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.DESERT_HILLS));
        options.included = new String[]{"minecraft:desert", "#minecraft:is_forest"};
        assertFalse(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.DESERT));
    }

    @Test
    public void juniperDefaultsAllowWoodlandAndHillsWithPrecipitation() {
        assertTrue(PlantBiomes.juniper(Biomes.FOREST));
        assertTrue(PlantBiomes.juniper(Biomes.MUTATED_FOREST));
        assertTrue(PlantBiomes.juniper(Biomes.BIRCH_FOREST));
        assertTrue(PlantBiomes.juniper(Biomes.ROOFED_FOREST));
        assertTrue(PlantBiomes.juniper(Biomes.EXTREME_HILLS));
        assertTrue(PlantBiomes.juniper(Biomes.EXTREME_HILLS_WITH_TREES));
        assertFalse(PlantBiomes.juniper(Biomes.PLAINS));
        assertFalse(PlantBiomes.juniper(Biomes.DESERT_HILLS));
        assertFalse(PlantBiomes.juniper(Biomes.SAVANNA));
    }

    @Test
    public void vanillaForestAndHillTagsHaveSeparateMembership() {
        PSConfig.Feature forest = new PSConfig.Feature();
        forest.included = new String[]{"#minecraft:is_forest"};
        PSConfig.Feature hills = new PSConfig.Feature();
        hills.included = new String[]{"#minecraft:is_hill"};
        Biome[] forests = {Biomes.FOREST, Biomes.FOREST_HILLS, Biomes.MUTATED_FOREST,
                Biomes.ROOFED_FOREST, Biomes.MUTATED_ROOFED_FOREST, Biomes.BIRCH_FOREST, Biomes.BIRCH_FOREST_HILLS,
                Biomes.MUTATED_BIRCH_FOREST, Biomes.MUTATED_BIRCH_FOREST_HILLS};
        Biome[] mountains = {Biomes.EXTREME_HILLS, Biomes.EXTREME_HILLS_EDGE, Biomes.EXTREME_HILLS_WITH_TREES,
                Biomes.MUTATED_EXTREME_HILLS, Biomes.MUTATED_EXTREME_HILLS_WITH_TREES};
        for (Biome biome : forests) {
            assertTrue(PlantBiomes.allows(forest, ignored -> false, biome));
            assertFalse(PlantBiomes.allows(hills, ignored -> false, biome));
        }
        for (Biome biome : mountains) {
            assertFalse(PlantBiomes.allows(forest, ignored -> false, biome));
            assertTrue(PlantBiomes.allows(hills, ignored -> false, biome));
            assertTrue(PlantBiomes.juniper(biome));
        }
    }

    @Test
    public void megaSpruceTaigaRequiresAnExplicitOverride() {
        PSConfig.Feature options = new PSConfig.Feature();
        for (Biome biome : new Biome[]{Biomes.MUTATED_REDWOOD_TAIGA, Biomes.MUTATED_REDWOOD_TAIGA_HILLS}) {
            assertFalse(PlantBiomes.tallCrops(biome));
            assertFalse(PlantBiomes.juniper(biome));
        }
        options.included = new String[]{"#forge:FOREST"};
        assertTrue(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.MUTATED_REDWOOD_TAIGA));
        assertTrue(PlantBiomes.allows(options, PlantBiomes::juniper, Biomes.MUTATED_REDWOOD_TAIGA_HILLS));
    }

    @Test
    public void badlandsTagIncludesEveryMesaVariant() {
        PSConfig.Feature badlands = new PSConfig.Feature();
        badlands.included = new String[]{"#minecraft:is_badlands"};
        Biome[] mesas = {Biomes.MESA, Biomes.MESA_ROCK, Biomes.MESA_CLEAR_ROCK, Biomes.MUTATED_MESA,
                Biomes.MUTATED_MESA_ROCK, Biomes.MUTATED_MESA_CLEAR_ROCK};
        for (Biome biome : mesas) {
            assertTrue(PlantBiomes.allows(badlands, ignored -> false, biome));
            assertTrue(PlantBiomes.agave(biome));
            assertTrue(PlantBiomes.peyote(biome));
        }
        assertFalse(PlantBiomes.allows(badlands, ignored -> false, Biomes.DESERT));
    }

    @Test
    public void savannaAndDesertSelectorsMatchTheVanillaFamilies() {
        PSConfig.Feature savanna = new PSConfig.Feature();
        savanna.included = new String[]{"#minecraft:is_savanna"};
        for (Biome biome : new Biome[]{Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU,
                Biomes.MUTATED_SAVANNA, Biomes.MUTATED_SAVANNA_ROCK}) {
            assertTrue(PlantBiomes.allows(savanna, ignored -> false, biome));
            assertTrue(PlantBiomes.peyote(biome));
        }
        assertFalse(PlantBiomes.allows(savanna, ignored -> false, Biomes.MESA));

        PSConfig.Feature desert = new PSConfig.Feature();
        desert.included = new String[]{"#minecraft:has_structure/desert_pyramid"};
        for (Biome biome : new Biome[]{Biomes.DESERT, Biomes.DESERT_HILLS, Biomes.MUTATED_DESERT}) {
            assertTrue(PlantBiomes.allows(desert, ignored -> false, biome));
        }
        assertFalse(PlantBiomes.allows(desert, ignored -> false, Biomes.SAVANNA));
    }

    @Test
    public void juniperKeepsConfigurableOverrides() {
        PSConfig.Feature options = new PSConfig.Feature();
        options.included = new String[]{"minecraft:plains"};
        assertTrue(PlantBiomes.allows(options, PlantBiomes::juniper, Biomes.PLAINS));
        options.excluded = new String[]{"#minecraft:is_forest"};
        assertFalse(PlantBiomes.allows(options, PlantBiomes::juniper, Biomes.FOREST));
        assertTrue(PlantBiomes.allows(options, PlantBiomes::juniper, Biomes.EXTREME_HILLS));
        options.enabled = false;
        assertFalse(PlantBiomes.allows(options, PlantBiomes::juniper, Biomes.EXTREME_HILLS));
        assertFalse(PlantBiomes.allows(options, PlantBiomes::juniper, Biomes.PLAINS));
    }
}
