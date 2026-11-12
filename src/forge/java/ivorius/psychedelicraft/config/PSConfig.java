package ivorius.psychedelicraft.config;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraftforge.common.config.Config;

@Config(modid = Psychedelicraft.MOD_ID)
public final class PSConfig {
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

        public final Generation worldGeneration = new Generation();
    }

    public static final class Generation {
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
