package ivorius.psychedelicraft.config;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraftforge.common.config.Config;

@Config(modid = Psychedelicraft.MOD_ID)
public final class PSConfig {
    private static final int MINUTE = 60 * 20;
    public static final Balancing balancing = new Balancing();
    public static final Visual visual = new Visual();

    private PSConfig() {
    }

    public static final class Visual {
        public boolean shader2DEnabled = true;
        public boolean doHeatDistortion = true;
        public boolean doWaterDistortion = true;
        public boolean waterOverlayEnabled = true;
        public boolean hurtOverlayEnabled = true;
        public boolean doMotionBlur = true;
        @Config.Comment("Blur intensity while the singleplayer game is paused.")
        public float pauseMenuBlur;
        @Config.Comment("Near focus distance in blocks.")
        public float dofFocalPointNear = 0.2F;
        @Config.Comment("Far focus distance in blocks.")
        public float dofFocalPointFar = 128F;
        public float dofFocalBlurNear;
        public float dofFocalBlurFar;
        @Config.RangeDouble(min = 0.01, max = 1)
        public float digitalEffectPixelRescaleX = 0.05F;
        @Config.RangeDouble(min = 0.01, max = 1)
        public float digitalEffectPixelRescaleY = 0.05F;
    }

    public static final class Balancing {
        @Config.Comment("Random chance per player tick to spawn a nearby Reality Rift, expressed as one in this many ticks. Zero disables spawning.")
        public int randomTicksUntilRiftSpawn = 180 * MINUTE;

        @Config.Comment("Show empty and filled Rift Jar variants in the creative inventory.")
        public boolean enableRiftJars;

        @Config.Comment("Hide the Molotov Cocktail variants in the creative inventory.")
        public boolean disableMolotovs;

        @Config.Comment("Show colored Harmonium variants in the creative inventory and enable dealer offers for them.")
        public boolean enableHarmonium;

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
        public final FluidTicks wheatHop = new FluidTicks(30, 60, 100, 30);
        public final FluidTicks agave = new FluidTicks(30, 80, 40, 90);
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
        @Config.RangeInt(min = 1, max = Integer.MAX_VALUE / 2)
        public int flowerExtractDistillationTicks = MINUTE;
    }

    public static final class FluidTicks {
        @Config.RangeInt(min = 1)
        public int ticksPerFermentation;
        @Config.RangeInt(min = 1)
        public int ticksPerDistillation;
        @Config.RangeInt(min = 1)
        public int ticksPerMaturation;
        @Config.RangeInt(min = 1)
        public int ticksUntilAcetification;

        FluidTicks() {
            this(40, 40, 30, 30);
        }

        FluidTicks(int fermentationMinutes, int distillationMinutes, int maturationMinutes, int acetificationMinutes) {
            ticksPerFermentation = fermentationMinutes * MINUTE;
            ticksPerDistillation = distillationMinutes * MINUTE;
            ticksPerMaturation = maturationMinutes * MINUTE;
            ticksUntilAcetification = acetificationMinutes * MINUTE;
        }
    }

    public static final class Generation {
        @Config.Comment("Generate copper ore in new Overworld chunks. The machines also accept ingotCopper from other mods.")
        public boolean copperOre = true;

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
