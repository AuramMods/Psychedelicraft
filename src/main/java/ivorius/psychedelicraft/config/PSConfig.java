package ivorius.psychedelicraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PSConfig {
    public static final ForgeConfigSpec COMMON;
    public static final ForgeConfigSpec.IntValue DRYING_TABLE_TICKS;
    public static final ForgeConfigSpec.IntValue IRON_DRYING_TABLE_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HARMONIUM;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MOLOTOVS;
    public static final Map<String,FluidTickConfig> FLUID_TICKS=new LinkedHashMap<>();
    public static final ForgeConfigSpec.IntValue FLOWER_EXTRACT_DISTILLATION_TICKS;
    public static final ForgeConfigSpec.IntValue SLURRY_HARDENING_TICKS;
    public static final Map<String,GenerationFeature> GENERATION = new LinkedHashMap<>();
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("worldGeneration");
        for (String name : List.of("cannabis","hop","tobacco","coffea","coca","belladonna","jimsonweed","tomato","peyote","morning_glory","juniper")) {
            builder.push(name);
            GENERATION.put(name,new GenerationFeature(builder.define("enabled",true),
                    builder.comment("Additional biome ids or #biome tags. All included selectors must match; built-in defaults remain enabled.")
                            .defineListAllowEmpty("included",List.of(),value -> value instanceof String),
                    builder.comment("Any matching biome id or #biome tag prevents generation.")
                            .defineListAllowEmpty("excluded",List.of(),value -> value instanceof String)));
            builder.pop();
        }
        builder.pop();
        builder.push("balancing");
        ENABLE_HARMONIUM=builder.define("enableHarmonium",true);
        DISABLE_MOLOTOVS=builder.comment("Hide molotovs in the creative catalog, matching the accepted setting.").define("disableMolotovs",false);
        DRYING_TABLE_TICKS=builder.comment("Ticks at maximum heat to finish a wooden drying-table batch.")
                .defineInRange("dryingTableTickDuration",16*60*20,1,Integer.MAX_VALUE);
        IRON_DRYING_TABLE_TICKS=builder.comment("Ticks at maximum heat to finish an iron drying-table batch.")
                .defineInRange("ironDryingTableTickDuration",12*60*20,1,Integer.MAX_VALUE);
        SLURRY_HARDENING_TICKS=builder.defineInRange("slurryHardeningTime",30*60*20,1,Integer.MAX_VALUE);
        builder.push("fluidAttributes");
        for(String name:List.of("wheatHop","wheat","potato","tomato","redGrapes","rice","juniper","honey","sugarCane","corn","apple","pineapple","banana","milk","agave")){
            builder.push(name);
            int[] minutes=name.equals("wheatHop")?new int[]{30,60,100,30}:name.equals("agave")?new int[]{30,80,40,90}:new int[]{40,40,30,30};
            FLUID_TICKS.put(name.replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(java.util.Locale.ROOT),new FluidTickConfig(
                    builder.defineInRange("ticksPerFermentation",minutes[0]*1200,1,Integer.MAX_VALUE),
                    builder.defineInRange("ticksPerDistillation",minutes[1]*1200,1,Integer.MAX_VALUE),
                    builder.defineInRange("ticksPerMaturation",minutes[2]*1200,1,Integer.MAX_VALUE),
                    builder.defineInRange("ticksUntilAcetification",minutes[3]*1200,1,Integer.MAX_VALUE)));
            builder.pop();
        }
        FLOWER_EXTRACT_DISTILLATION_TICKS=builder.defineInRange("flowerExtractDistillationTicks",1200,1,Integer.MAX_VALUE/2);
        builder.pop();
        builder.pop();
        COMMON=builder.build();
    }
    private PSConfig() {}
    public record FluidTickConfig(ForgeConfigSpec.IntValue fermentation,ForgeConfigSpec.IntValue distillation,ForgeConfigSpec.IntValue maturation,ForgeConfigSpec.IntValue acetification){
        public FluidTicks snapshot(){return new FluidTicks(fermentation.get(),distillation.get(),maturation.get(),acetification.get());}
    }
    public static final class FluidTicks {
        public final int ticksPerFermentation,ticksPerDistillation,ticksPerMaturation,ticksUntilAcetification;
        FluidTicks(int fermentation,int distillation,int maturation,int acetification){ticksPerFermentation=fermentation;ticksPerDistillation=distillation;ticksPerMaturation=maturation;ticksUntilAcetification=acetification;}
    }
    public record GenerationFeature(ForgeConfigSpec.BooleanValue enabled,
                                    ForgeConfigSpec.ConfigValue<List<? extends String>> included,
                                    ForgeConfigSpec.ConfigValue<List<? extends String>> excluded) {}
}
