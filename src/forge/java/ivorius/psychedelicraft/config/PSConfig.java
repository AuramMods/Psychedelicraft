package ivorius.psychedelicraft.config;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraftforge.common.config.Config;

@Config(modid = Psychedelicraft.MOD_ID)
public final class PSConfig {
    private static final int MINUTE = 60 * 20;
    public static final Balancing balancing = new Balancing();

    private PSConfig() {
    }

    public static final class Balancing {
        @Config.Comment("Ticks at maximum heat to finish one wooden drying table batch.")
        @Config.RangeInt(min = 1)
        public int dryingTableTickDuration = 16 * 60 * 20;

        @Config.Comment("Ticks at maximum heat to finish one iron drying table batch.")
        @Config.RangeInt(min = 1)
        public int ironDryingTableTickDuration = 12 * 60 * 20;

        @Config.RangeInt(min = 1)
        public int slurryHardeningTime = 30 * MINUTE;

        public final FluidProperties fluidAttributes = new FluidProperties();
        public final Generation worldGeneration = new Generation();
    }

    public static final class FluidProperties {
        public final FluidTicks wheatHop = new FluidTicks(30, 100, 30);
        public final FluidTicks agave = new FluidTicks(30, 40, 90);
        public final FluidTicks wheat = new FluidTicks();
        public final FluidTicks potato = new FluidTicks();
        public final FluidTicks tomato = new FluidTicks();
        public final FluidTicks redGrapes = new FluidTicks();
        public final FluidTicks rice = new FluidTicks();
        public final FluidTicks juniper = new FluidTicks();
        public final FluidTicks honey = new FluidTicks();
        public final FluidTicks sugarCane = new FluidTicks();
        public final FluidTicks corn = new FluidTicks();
        public final FluidTicks apple = new FluidTicks();
        public final FluidTicks pineapple = new FluidTicks();
        public final FluidTicks banana = new FluidTicks();
        public final FluidTicks milk = new FluidTicks();
    }

    public static final class FluidTicks {
        @Config.RangeInt(min = 1)
        public int ticksPerFermentation;
        @Config.RangeInt(min = 1)
        public int ticksPerMaturation;
        @Config.RangeInt(min = 1)
        public int ticksUntilAcetification;

        FluidTicks() {
            this(40, 30, 30);
        }

        FluidTicks(int fermentationMinutes, int maturationMinutes, int acetificationMinutes) {
            ticksPerFermentation = fermentationMinutes * MINUTE;
            ticksPerMaturation = maturationMinutes * MINUTE;
            ticksUntilAcetification = acetificationMinutes * MINUTE;
        }
    }

    public static final class Generation {
        @Config.Comment("Allow novice farmers to sell Psychedelicraft goods.")
        public boolean farmerDrugDeals = true;

        @Config.Comment("Add Psychedelicraft loot to dungeon and mineshaft chests when loot tables load.")
        public boolean dungeonChests = true;

        @Config.Comment("Add Psychedelicraft loot to village blacksmith chests when loot tables load.")
        public boolean villageChests = true;

        public final Feature juniper = new Feature();
        public final Feature cannabis = new Feature();
        public final Feature hop = new Feature();
        public final Feature tobacco = new Feature();
        public final Feature coffea = new Feature();
        public final Feature coca = new Feature();
        public final Feature morningGlories = new Feature();
        public final Feature belladonna = new Feature();
        public final Feature jimsonweed = new Feature();
        public final Feature tomato = new Feature();
        @Config.Comment("Biome selection for both peyote and agave.")
        public final Feature peyote = new Feature();
    }

    public static final class Feature {
        public boolean enabled = true;
        @Config.Comment("Add biomes matching all selectors. Use biome IDs, supported Minecraft biome tags, or #forge:TYPE.")
        public String[] included = {};
        @Config.Comment("Exclude any biome matching one of these selectors.")
        public String[] excluded = {};
    }
}
