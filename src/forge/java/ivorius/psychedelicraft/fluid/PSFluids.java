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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PSFluids {
    private static final String FORGE_PREFIX = Psychedelicraft.MOD_ID + "_";
    private static final Map<String, Fluid> FLUIDS = new LinkedHashMap<>();

    public static final Fluid WHEAT_HOP = fluid("wheat_hop", "beer", 0xaafeaa08, 1);
    public static final Fluid WHEAT = fluid("wheat", "beer", 0xaafeaa08, 1);
    public static final Fluid POTATO = fluid("potato", "beer", 0xaafeaa08, 2);
    public static final Fluid TOMATO = fluid("tomato", "tomato_juice", 0xaaffaa08, 1);
    public static final Fluid RED_GRAPES = fluid("red_grapes", "wine", 0xaafeaa08, 1);
    public static final Fluid RICE = fluid("rice", "rice_wine", 0xeecac4b2, 1);
    public static final Fluid JUNIPER = fluid("juniper", "slurry", 0xcc704e21, 1);
    public static final Fluid HONEY = fluid("honey", "mead", 0xbbe9ae3b, 5);
    public static final Fluid SUGAR_CANE = fluid("sugar_cane", "clear", 0xaafeaa08, 1);
    public static final Fluid CORN = fluid("corn", "beer", 0xaafeaa08, 1);
    public static final Fluid APPLE = fluid("apple", "cider", 0x99edc13b, 1);
    public static final Fluid PINEAPPLE = fluid("pineapple", "cider", 0x99edc13b, 2);
    public static final Fluid BANANA = fluid("banana", "mead", 0xbbe9ae3b, 3);
    public static final Fluid MILK = fluid("milk", "rice_wine", 0x88ffffff, 2);
    public static final Fluid AGAVE = fluid("agave", "tea", 0x779bab62, 1);
    public static final Fluid COFFEE = fluid("coffee", "coffee", 0xffa77d55, 1);
    public static final Fluid COCA_TEA = fluid("coca_tea", "tea", 0x44787a36, 1);
    public static final Fluid CANNABIS_TEA = fluid("cannabis_tea", "tea", 0x446d6f3c, 1);
    public static final Fluid PEYOTE_JUICE = fluid("peyote_juice", "tea", 0x779bab62, 2);
    public static final Fluid KAVA = fluid("kava", "tea", 0x779bab62, 1);
    public static final Fluid COCAINE = fluid("cocaine", "clear", 0x44e8f4f8, 1);
    public static final Fluid CAFFEINE = fluid("caffeine", "clear", 0x66eee2d3, 1);
    public static final Fluid BATH_SALTS = fluid("bath_salts", "clear", 0x2233f4f8, 1);
    public static final Fluid MORNING_GLORY_EXTRACT = fluid("morning_glory_extract", "clear", 0x66eee2d3, 1);
    public static final Fluid BELLADONNA_EXTRACT = fluid("belladonna_extract", "clear", 0x66eee2d3, 1);
    public static final Fluid JIMSONWEED_EXTRACT = fluid("jimsonweed_extract", "clear", 0x66eee2d3, 1);
    public static final Fluid SLURRY = fluid("slurry", "slurry", 0xcc704e21, 4);

    private PSFluids() {
    }

    private static Fluid fluid(String path, String texture, int color, int viscosity) {
        String id = Psychedelicraft.id(path).toString();
        String sprite = "blocks/fluid/" + texture;
        Fluid fluid = new Fluid(FORGE_PREFIX + path, Psychedelicraft.id(sprite + "_still"), Psychedelicraft.id(sprite + "_flow"), color)
                .setUnlocalizedName("psychedelicraft." + path).setViscosity(1000 * viscosity);
        FLUIDS.put(id, fluid);
        return fluid;
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

    public static boolean canConsume(Fluid fluid, boolean injection) {
        return injection ? fluid == CAFFEINE : fluid == COFFEE || fluid == PEYOTE_JUICE;
    }

    public static void consume(EntityPlayer player, FluidStack portion) {
        DrugProperties properties = DrugCapability.get(player);
        if (properties == null) return;
        double buckets = portion.amount / (double) Fluid.BUCKET_VOLUME;
        Fluid fluid = portion.getFluid();
        if (fluid == COFFEE) {
            double warmth = warmth(portion) / 2.0;
            properties.addInfluence(new DrugInfluence(DrugType.CAFFEINE, 20, 0.002, 0.001, (0.25 + warmth * 0.05) * buckets));
            if (warmth > 0) {
                properties.addInfluence(new DrugInfluence(DrugType.WARMTH, 0, 0, 0.1, 0.8 * warmth * buckets));
            }
        } else if (fluid == PEYOTE_JUICE) {
            properties.addInfluence(new DrugInfluence(DrugType.PEYOTE, 15, 0.005, 0.003, 2 * buckets));
        } else if (fluid == CAFFEINE) {
            properties.addInfluence(new DrugInfluence(DrugType.CAFFEINE, 0, 0.005, 0.01, 85 * buckets));
        }
    }

    private static int warmth(FluidStack stack) {
        return stack.tag == null ? 0 : MathHelper.clamp(stack.tag.getInteger("warmth"), 0, 2);
    }

    public static String name(FluidStack stack) {
        return stack.getFluid() == COFFEE ? "fluid.psychedelicraft.coffee.temperature." + warmth(stack)
                : stack.getUnlocalizedName();
    }
}
