package ivorius.psychedelicraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PSConfig {
    public static final ForgeConfigSpec COMMON;
    public static final ForgeConfigSpec.BooleanValue FARMER_DRUG_DEALS;
    public static final ForgeConfigSpec.BooleanValue VILLAGE_CHESTS,DUNGEON_CHESTS;
    public static final ForgeConfigSpec.IntValue DRYING_TABLE_TICKS;
    public static final ForgeConfigSpec.IntValue IRON_DRYING_TABLE_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HARMONIUM;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MOLOTOVS,ENABLE_RIFT_JARS;
    public static final ForgeConfigSpec.BooleanValue DISTORT_INCOMING_MESSAGES,DISTORT_OUTGOING_MESSAGES;
    public static final ForgeConfigSpec.IntValue RIFT_SPAWN_INTERVAL;
    public static final Map<String,FluidTickConfig> FLUID_TICKS=new LinkedHashMap<>();
    public static final ForgeConfigSpec.IntValue FLOWER_EXTRACT_DISTILLATION_TICKS;
    public static final ForgeConfigSpec.IntValue SLURRY_HARDENING_TICKS;
    public static final Map<String,GenerationFeature> GENERATION = new LinkedHashMap<>();
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("worldGeneration");
        FARMER_DRUG_DEALS=builder.comment("Add the original crop and drug trades to the native novice farmer pool.").define("farmerDrugDeals",true);
        VILLAGE_CHESTS=builder.comment("Add mod supplies to the five original native village chest profiles.").define("villageChests",true);
        DUNGEON_CHESTS=builder.comment("Add mod supplies to dungeon and abandoned mineshaft chests.").define("dungeonChests",true);
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
        ENABLE_RIFT_JARS=builder.comment("Show empty and filled rift jars in the creative catalog.").define("enableRiftJars",false);
        RIFT_SPAWN_INTERVAL=builder.comment("One-in-N chance per player tick to spawn a nearby Reality Rift. Zero disables random spawning.").defineInRange("randomTicksUntilRiftSpawn",180*60*20,0,Integer.MAX_VALUE);
        ENABLE_HARMONIUM=builder.define("enableHarmonium",false);
        DISABLE_MOLOTOVS=builder.comment("Hide molotovs in the creative catalog, matching the accepted setting.").define("disableMolotovs",false);
        DRYING_TABLE_TICKS=builder.comment("Ticks at maximum heat to finish a wooden drying-table batch.")
                .defineInRange("dryingTableTickDuration",16*60*20,1,Integer.MAX_VALUE);
        IRON_DRYING_TABLE_TICKS=builder.comment("Ticks at maximum heat to finish an iron drying-table batch.")
                .defineInRange("ironDryingTableTickDuration",12*60*20,1,Integer.MAX_VALUE);
        SLURRY_HARDENING_TICKS=builder.defineInRange("slurryHardeningTime",30*60*20,1,Integer.MAX_VALUE);
        builder.push("messageDistortion");
        DISTORT_INCOMING_MESSAGES=builder.comment("Distort locally displayed chat according to active alcohol/kava, cannabis and Zero effects.").define("incoming",true);
        DISTORT_OUTGOING_MESSAGES=builder.comment("Distort chat before signing and sending it. Commands remain unchanged.").define("outgoing",true);
        builder.pop();
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
