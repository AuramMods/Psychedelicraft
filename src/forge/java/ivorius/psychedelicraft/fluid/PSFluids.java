/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PSFluids {
    private static final String FORGE_PREFIX = Psychedelicraft.MOD_ID + "_";
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();

    public static final BrewFluid WHEAT_HOP = register("wheat_hop", BrewProfiles.wheatHop());
    public static final BrewFluid WHEAT = register("wheat", BrewProfiles.wheat());
    public static final BrewFluid POTATO = register("potato", BrewProfiles.potato());
    public static final BrewFluid TOMATO = register("tomato", BrewProfiles.tomato());
    public static final BrewFluid RED_GRAPES = register("red_grapes", BrewProfiles.redGrapes());
    public static final BrewFluid RICE = register("rice", BrewProfiles.rice());
    public static final BrewFluid JUNIPER = register("juniper", BrewProfiles.juniper());
    public static final BrewFluid HONEY = register("honey", BrewProfiles.honey());
    public static final BrewFluid SUGAR_CANE = register("sugar_cane", BrewProfiles.sugarCane());
    public static final BrewFluid CORN = register("corn", BrewProfiles.corn());
    public static final BrewFluid APPLE = register("apple", BrewProfiles.apple());
    public static final BrewFluid PINEAPPLE = register("pineapple", BrewProfiles.pineapple());
    public static final BrewFluid BANANA = register("banana", BrewProfiles.banana());
    public static final BrewFluid MILK = register("milk", BrewProfiles.milk());
    public static final BrewFluid AGAVE = register("agave", BrewProfiles.agave());
    public static final Fluid COFFEE = fluid("coffee", "coffee", 0xffa77d55, 1);
    public static final Fluid COCA_TEA = fluid("coca_tea", "tea", 0x44787a36, 1);
    public static final Fluid CANNABIS_TEA = fluid("cannabis_tea", "tea", 0x446d6f3c, 1);
    public static final Fluid PEYOTE_JUICE = fluid("peyote_juice", "tea", 0x779bab62, 2);
    public static final Fluid KAVA = fluid("kava", "tea", 0x779bab62, 1);
    public static final Fluid COCAINE = fluid("cocaine", "clear", 0x44e8f4f8, 1);
    public static final Fluid CAFFEINE = fluid("caffeine", "clear", 0x66eee2d3, 1);
    public static final Fluid BATH_SALTS = fluid("bath_salts", "clear", 0x2233f4f8, 1);
    public static final Fluid MORNING_GLORY_EXTRACT = extract("morning_glory_extract");
    public static final Fluid BELLADONNA_EXTRACT = extract("belladonna_extract");
    public static final Fluid JIMSONWEED_EXTRACT = extract("jimsonweed_extract");
    public static final Fluid SLURRY = fluid("slurry", "slurry", 0xcc704e21, 4);

    private PSFluids() {
    }

    private static Fluid fluid(String path, String texture, int color, int viscosity) {
        String sprite = "blocks/fluid/" + texture;
        Fluid fluid = new Fluid(FORGE_PREFIX + path, Psychedelicraft.id(sprite + "_still"), Psychedelicraft.id(sprite + "_flow"), color)
                .setUnlocalizedName("psychedelicraft." + path).setViscosity(1000 * viscosity);
        return register(path, fluid);
    }

    private static <T extends Fluid> T register(String path, T fluid) {
        FLUIDS.put(Psychedelicraft.id(path).toString(), fluid);
        return fluid;
    }

    private static Fluid extract(String path) {
        return register(path, new Fluid(FORGE_PREFIX + path,
                BrewFluid.texture("clear", "_still"), BrewFluid.texture("clear", "_flow"), 0x66eee2d3) {
            @Override
            public int getColor(FluidStack stack) {
                return BrewFluid.blend(super.getColor(stack), 0xffffffff,
                        BrewFluid.stage(stack, BrewFluid.DISTILLATION, 2) / 16F);
            }
        }.setUnlocalizedName(Psychedelicraft.MOD_ID + "." + path));
    }

    public static void register() {
        for (Fluid fluid : FLUIDS.values()) {
            FluidRegistry.registerFluid(fluid);
        }
    }

    @Nullable
    public static Fluid resolve(String id) {
        if ("minecraft:water".equals(id)) return FluidRegistry.WATER;
        if ("minecraft:lava".equals(id)) return FluidRegistry.LAVA;
        Fluid fluid = FLUIDS.get(id);
        return fluid == null ? FluidRegistry.getFluid(id) : fluid;
    }

    public static String id(Fluid fluid) {
        if (fluid == FluidRegistry.WATER) return "minecraft:water";
        if (fluid == FluidRegistry.LAVA) return "minecraft:lava";
        String name = fluid.getName();
        if (name.startsWith(FORGE_PREFIX)) {
            String id = Psychedelicraft.id(name.substring(FORGE_PREFIX.length())).toString();
            if (FLUIDS.get(id) == fluid) return id;
        }
        return name;
    }

    public static boolean isInjectable(Fluid fluid) {
        return fluid == COCAINE || fluid == CAFFEINE || fluid == BATH_SALTS || fluid == MORNING_GLORY_EXTRACT;
    }

    public static boolean isFlowerExtract(Fluid fluid) {
        return fluid == MORNING_GLORY_EXTRACT || fluid == BELLADONNA_EXTRACT || fluid == JIMSONWEED_EXTRACT;
    }

    public static boolean canConsume(Fluid fluid, boolean injection) {
        return injection ? isInjectable(fluid)
                : fluid instanceof BrewFluid || isFlowerExtract(fluid) || fluid == COFFEE || fluid == COCA_TEA
                || fluid == CANNABIS_TEA || fluid == PEYOTE_JUICE || fluid == KAVA;
    }

    public static void consume(EntityPlayer player, FluidStack portion) {
        DrugProperties properties = DrugCapability.get(player);
        if (properties != null) addServingInfluences(portion, properties);
    }

    public static void addServingInfluences(FluidStack portion, DrugProperties properties) {
        double buckets = portion.amount / (double) Fluid.BUCKET_VOLUME;
        Fluid fluid = portion.getFluid();
        if (fluid instanceof BrewFluid) {
            ((BrewFluid) fluid).addServingInfluences(portion, buckets, properties);
        } else if (isFlowerExtract(fluid)) {
            int concentration = BrewFluid.stage(portion, BrewFluid.DISTILLATION, 2);
            DrugType type = fluid == MORNING_GLORY_EXTRACT ? DrugType.LSD : DrugType.ATROPINE;
            properties.addInfluence(new DrugInfluence(type, 3, 0, 0.03, Math.pow(96, concentration) * buckets));
        } else if (fluid == COFFEE) {
            double warmth = warmth(portion) / 2.0;
            properties.addInfluence(new DrugInfluence(DrugType.CAFFEINE, 20, 0.002, 0.001, (0.25 + warmth * 0.05) * buckets));
            if (warmth > 0) {
                properties.addInfluence(new DrugInfluence(DrugType.WARMTH, 0, 0, 0.1, 0.8 * warmth * buckets));
            }
        } else if (fluid == PEYOTE_JUICE) {
            properties.addInfluence(new DrugInfluence(DrugType.PEYOTE, 15, 0.005, 0.003, 2 * buckets));
        } else if (fluid == CAFFEINE) {
            properties.addInfluence(new DrugInfluence(DrugType.CAFFEINE, 0, 0.005, 0.01, 85 * buckets));
        } else if (fluid == COCA_TEA) {
            properties.addInfluence(new DrugInfluence(DrugType.COCAINE, 60, 0.005, 0.002, 0.2 * buckets));
        } else if (fluid == CANNABIS_TEA) {
            properties.addInfluence(new DrugInfluence(DrugType.CANNABIS, 60, 0.005, 0.002, 0.25 * buckets));
        } else if (fluid == KAVA) {
            properties.addInfluence(new DrugInfluence(DrugType.KAVA, 20, 0.005, 0.003, 2 * buckets));
        } else if (fluid == COCAINE) {
            properties.addInfluence(new DrugInfluence(DrugType.COCAINE, 0, 0.005, 0.01, 50 * buckets));
        } else if (fluid == BATH_SALTS) {
            properties.addInfluence(new DrugInfluence(DrugType.BATH_SALTS, 0, 0.005, 0.01, 50 * buckets));
        }
    }

    public static int warmth(FluidStack stack) {
        return stack.tag == null ? 0 : MathHelper.clamp(stack.tag.getInteger("warmth"), 0, 2);
    }

    public static String displayName(FluidStack stack) {
        if (isFlowerExtract(stack.getFluid())) {
            return I18n.translateToLocal(stack.getUnlocalizedName() + ".distilled."
                    + BrewFluid.stage(stack, BrewFluid.DISTILLATION, 2));
        }
        return stack.getFluid() == COFFEE
                ? I18n.translateToLocal("fluid.psychedelicraft.coffee.temperature." + warmth(stack))
                : stack.getLocalizedName();
    }
}
