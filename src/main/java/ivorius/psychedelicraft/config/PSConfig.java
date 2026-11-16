package ivorius.psychedelicraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PSConfig {
    public static final ForgeConfigSpec COMMON;
    public static final Map<String,GenerationFeature> GENERATION = new LinkedHashMap<>();
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("worldGeneration");
        for (String name : List.of("cannabis","hop","tobacco","coffea","coca","belladonna","jimsonweed","tomato","peyote")) {
            builder.push(name);
            GENERATION.put(name,new GenerationFeature(builder.define("enabled",true),
                    builder.comment("Additional biome ids or #biome tags. All included selectors must match; built-in defaults remain enabled.")
                            .defineListAllowEmpty("included",List.of(),value -> value instanceof String),
                    builder.comment("Any matching biome id or #biome tag prevents generation.")
                            .defineListAllowEmpty("excluded",List.of(),value -> value instanceof String)));
            builder.pop();
        }
        builder.pop();
        COMMON=builder.build();
    }
    private PSConfig() {}
    public record GenerationFeature(ForgeConfigSpec.BooleanValue enabled,
                                    ForgeConfigSpec.ConfigValue<List<? extends String>> included,
                                    ForgeConfigSpec.ConfigValue<List<? extends String>> excluded) {}
}
