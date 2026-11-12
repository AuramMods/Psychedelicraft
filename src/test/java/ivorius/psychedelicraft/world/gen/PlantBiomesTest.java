package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
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
        assertTrue(PlantBiomes.belladonna(Biomes.ROOFED_FOREST));
        assertTrue(PlantBiomes.jungle(Biomes.JUNGLE_EDGE));
        assertTrue(PlantBiomes.tomato(Biomes.FOREST_HILLS));
        assertFalse(PlantBiomes.tomato(Biomes.MUTATED_FOREST));
        assertTrue(PlantBiomes.peyote(Biomes.SAVANNA));
        assertTrue(PlantBiomes.agave(Biomes.MESA));
        assertFalse(PlantBiomes.agave(Biomes.ICE_PLAINS));
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
    public void allInclusionSelectorsMustMatchToExtendDefaults() {
        PSConfig.Feature options = new PSConfig.Feature();
        options.included = new String[]{"minecraft:desert", "#forge:SANDY"};
        assertTrue(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.DESERT));
        assertFalse(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.DESERT_HILLS));
        options.included = new String[]{"minecraft:desert", "#minecraft:is_forest"};
        assertFalse(PlantBiomes.allows(options, PlantBiomes::tallCrops, Biomes.DESERT));
    }
}
