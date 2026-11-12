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
    COOL;

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
            default:
                return contents.copy();
        }
        return result;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BrewProcess fromId(int id) {
        BrewProcess[] values = values();
        return id >= 0 && id < values.length ? values[id] : IDLE;
    }
}
