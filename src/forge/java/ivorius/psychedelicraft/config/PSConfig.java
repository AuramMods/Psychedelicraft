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
    }
}
