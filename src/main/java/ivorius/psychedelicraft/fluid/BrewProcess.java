/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Locale;

public enum BrewProcess {
    IDLE,
    FERMENT,
    ACETIFY,
    MATURE,
    HARDEN,
    COOL,
    DISTILL;

    public static final int SLURRY_PER_DIRT = 4 * Fluid.BUCKET_VOLUME;

    public static BrewProcess find(@Nullable FluidStack contents, boolean aging, boolean canCollectDirt) {
        if (contents == null) return IDLE;
        Fluid fluid = contents.getFluid();
        if (fluid == PSFluids.SLURRY && contents.amount >= SLURRY_PER_DIRT && canCollectDirt) return HARDEN;
        if (fluid == PSFluids.COFFEE && !aging && BrewFluid.stage(contents, "warmth", 2) == 2) return COOL;
        if (!(fluid instanceof BrewFluid) || BrewFluid.isVinegar(contents)) return IDLE;
        int fermented = BrewFluid.stage(contents, BrewFluid.FERMENTATION, BrewFluid.FERMENTATION_STEPS);
        if (!aging) return fermented < BrewFluid.FERMENTATION_STEPS ? FERMENT : ACETIFY;
        return fermented > 0 && BrewFluid.stage(contents, BrewFluid.MATURATION, BrewFluid.MATURATION_STEPS)
                < BrewFluid.MATURATION_STEPS ? MATURE : IDLE;
    }

    public int duration(FluidStack contents) {
        switch (this) {
            case FERMENT: return ((BrewFluid) contents.getFluid()).getTicks().ticksPerFermentation;
            case ACETIFY: return ((BrewFluid) contents.getFluid()).getTicks().ticksUntilAcetification;
            case MATURE: return ((BrewFluid) contents.getFluid()).getTicks().ticksPerMaturation;
            case HARDEN: return PSConfig.balancing.slurryHardeningTime;
            case COOL: return 300;
            case DISTILL:
                if (contents.getFluid() instanceof BrewFluid) {
                    return ((BrewFluid) contents.getFluid()).getTicks().ticksPerDistillation;
                }
                return PSConfig.balancing.fluidAttributes.flowerExtractDistillationTicks * (1 + distillation(contents));
            default: return 0;
        }
    }

    @Nullable
    public FluidStack finish(FluidStack contents) {
        FluidStack result = contents.copy();
        if (this == HARDEN) {
            result.amount -= SLURRY_PER_DIRT;
            return result.amount > 0 ? result : null;
        }
        if (this == DISTILL && PSFluids.isFlowerExtract(contents.getFluid())) {
            result.amount -= 2;
            return result.amount > 0 ? result : null;
        }
        if (result.tag == null) result.tag = new NBTTagCompound();
        switch (this) {
            case FERMENT:
                int fermented = BrewFluid.stage(contents, BrewFluid.FERMENTATION, BrewFluid.FERMENTATION_STEPS);
                result.tag.setInteger(BrewFluid.FERMENTATION, Math.min(fermented + 1, BrewFluid.FERMENTATION_STEPS));
                break;
            case ACETIFY:
                result.tag.setBoolean(BrewFluid.VINEGAR, true);
                break;
            case MATURE:
                int maturation = BrewFluid.stage(contents, BrewFluid.MATURATION, BrewFluid.MATURATION_STEPS);
                result.tag.setInteger(BrewFluid.MATURATION, Math.min(maturation + 1, BrewFluid.MATURATION_STEPS));
                break;
            case COOL:
                result.tag.setInteger("warmth", 1);
                break;
            case DISTILL:
                if (contents.amount > 1) result.amount--;
                result.tag.setInteger(BrewFluid.DISTILLATION, distillation(contents) + 1);
                break;
            default:
                return contents.copy();
        }
        return result;
    }

    public static BrewProcess findDistillation(@Nullable FluidStack contents) {
        if (contents == null) return IDLE;
        Fluid fluid = contents.getFluid();
        if (PSFluids.isFlowerExtract(fluid)) {
            return contents.amount >= 2 && distillation(contents) < 2 ? DISTILL : IDLE;
        }
        if (!(fluid instanceof BrewFluid)) return IDLE;
        return BrewFluid.stage(contents, BrewFluid.FERMENTATION, BrewFluid.FERMENTATION_STEPS) > 0
                && BrewFluid.stage(contents, BrewFluid.MATURATION, BrewFluid.MATURATION_STEPS) == 0
                && distillation(contents) < 16 ? DISTILL : IDLE;
    }

    @Nullable
    public static FluidStack distillationOutput(FluidStack contents) {
        if (PSFluids.isFlowerExtract(contents.getFluid())) {
            FluidStack output = new FluidStack(contents, 1);
            if (output.tag == null) output.tag = new NBTTagCompound();
            output.tag.setInteger(BrewFluid.DISTILLATION, distillation(contents) + 1);
            return output;
        }
        return contents.amount > 1 ? new FluidStack(PSFluids.SLURRY, 1) : null;
    }

    private static int distillation(FluidStack fluid) {
        return BrewFluid.stage(fluid, BrewFluid.DISTILLATION, PSFluids.isFlowerExtract(fluid.getFluid()) ? 2 : 16);
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BrewProcess fromId(int id) {
        BrewProcess[] values = values();
        return id >= 0 && id < values.length ? values[id] : IDLE;
    }
}
