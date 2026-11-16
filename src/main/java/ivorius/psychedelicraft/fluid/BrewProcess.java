/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidStack;

import java.util.Locale;

public enum BrewProcess {
    IDLE,
    FERMENT,
    ACETIFY,
    MATURE,
    HARDEN,
    COOL,
    DISTILL;

    public static final int SLURRY_PER_DIRT = 4 * FluidType.BUCKET_VOLUME;

    public static BrewProcess find(FluidStack contents, boolean aging, boolean canCollectDirt) {
        if (contents.isEmpty()) return IDLE;
        Fluid fluid = contents.getFluid();
        if (fluid == PSFluids.SLURRY.get() && contents.getAmount() >= SLURRY_PER_DIRT && canCollectDirt) return HARDEN;
        if (fluid == PSFluids.COFFEE.get() && !aging && BrewProfile.stage(contents, "warmth", 2) == 2) return COOL;
        if (PSFluids.brew(fluid) == null || BrewProfile.isVinegar(contents)) return IDLE;
        int fermented = BrewProfile.stage(contents, BrewProfile.FERMENTATION, BrewProfile.FERMENTATION_STEPS);
        if (!aging) return fermented < BrewProfile.FERMENTATION_STEPS ? FERMENT : ACETIFY;
        return fermented > 0 && BrewProfile.stage(contents, BrewProfile.MATURATION, BrewProfile.MATURATION_STEPS)
                < BrewProfile.MATURATION_STEPS ? MATURE : IDLE;
    }

    public int duration(FluidStack contents) {
        switch (this) {
            case FERMENT: return PSFluids.brew(contents.getFluid()).getTicks().ticksPerFermentation;
            case ACETIFY: return PSFluids.brew(contents.getFluid()).getTicks().ticksUntilAcetification;
            case MATURE: return PSFluids.brew(contents.getFluid()).getTicks().ticksPerMaturation;
            case HARDEN: return PSConfig.SLURRY_HARDENING_TICKS.get();
            case COOL: return 300;
            case DISTILL:
                if (PSFluids.brew(contents.getFluid()) != null) {
                    return PSFluids.brew(contents.getFluid()).getTicks().ticksPerDistillation;
                }
                return PSConfig.FLOWER_EXTRACT_DISTILLATION_TICKS.get() * (1 + distillation(contents));
            default: return 0;
        }
    }

    public FluidStack finish(FluidStack contents) {
        if(contents.isEmpty()||this==IDLE)return contents.copy();
        FluidStack result = contents.copy();
        if (this == HARDEN) {
            result.shrink(SLURRY_PER_DIRT);
            return result.isEmpty() ? FluidStack.EMPTY : result;
        }
        if (this == DISTILL && PSFluids.isFlowerExtract(contents.getFluid())) {
            result.shrink(2);
            return result.isEmpty() ? FluidStack.EMPTY : result;
        }
        CompoundTag attributes = result.getOrCreateTag();
        switch (this) {
            case FERMENT:
                int fermented = BrewProfile.stage(contents, BrewProfile.FERMENTATION, BrewProfile.FERMENTATION_STEPS);
                attributes.putInt(BrewProfile.FERMENTATION, Math.min(fermented + 1, BrewProfile.FERMENTATION_STEPS));
                break;
            case ACETIFY:
                attributes.putBoolean(BrewProfile.VINEGAR, true);
                break;
            case MATURE:
                int maturation = BrewProfile.stage(contents, BrewProfile.MATURATION, BrewProfile.MATURATION_STEPS);
                attributes.putInt(BrewProfile.MATURATION, Math.min(maturation + 1, BrewProfile.MATURATION_STEPS));
                break;
            case COOL:
                attributes.putInt("warmth", 1);
                break;
            case DISTILL:
                if (contents.getAmount() > 1) result.shrink(1);
                attributes.putInt(BrewProfile.DISTILLATION, distillation(contents) + 1);
                break;
            default:
                return contents.copy();
        }
        return result;
    }

    public static BrewProcess findDistillation(FluidStack contents) {
        if (contents.isEmpty()) return IDLE;
        Fluid fluid = contents.getFluid();
        if (PSFluids.isFlowerExtract(fluid)) {
            return contents.getAmount() >= 2 && distillation(contents) < 2 ? DISTILL : IDLE;
        }
        if (PSFluids.brew(fluid) == null) return IDLE;
        return BrewProfile.stage(contents, BrewProfile.FERMENTATION, BrewProfile.FERMENTATION_STEPS) > 0
                && BrewProfile.stage(contents, BrewProfile.MATURATION, BrewProfile.MATURATION_STEPS) == 0
                && distillation(contents) < 16 ? DISTILL : IDLE;
    }

    public static FluidStack distillationOutput(FluidStack contents) {
        if (PSFluids.isFlowerExtract(contents.getFluid())) {
            FluidStack output = new FluidStack(contents, 1);
            output.getOrCreateTag().putInt(BrewProfile.DISTILLATION, distillation(contents) + 1);
            return output;
        }
        return contents.getAmount() > 1 ? new FluidStack(PSFluids.SLURRY.get(), 1) : FluidStack.EMPTY;
    }

    private static int distillation(FluidStack fluid) {
        return BrewProfile.stage(fluid, BrewProfile.DISTILLATION, PSFluids.isFlowerExtract(fluid.getFluid()) ? 2 : 16);
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BrewProcess fromId(int id) {
        BrewProcess[] values = values();
        return id >= 0 && id < values.length ? values[id] : IDLE;
    }
}
