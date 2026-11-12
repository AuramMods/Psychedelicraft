/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig.FluidTicks;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BrewFluid extends Fluid {
    public static final int FERMENTATION_STEPS = 2;
    public static final int MATURATION_STEPS = 16;
    public static final String FERMENTATION = "fermentation";
    public static final String MATURATION = "maturation";
    public static final String DISTILLATION = "distillation";
    public static final String VINEGAR = "vinegar";

    private final Supplier<FluidTicks> ticks;
    private final int distilledColor;
    private final int matureColor;
    private final double fermentationAlcohol;
    private final double distillationAlcohol;
    private final double maturationAlcohol;
    private final Style[] styles;

    BrewFluid(String path, String texture, int color, int viscosity, Supplier<FluidTicks> ticks,
              int distilledColor, int matureColor, double fermentationAlcohol,
              double distillationAlcohol, double maturationAlcohol, Style... styles) {
        super(Psychedelicraft.MOD_ID + "_" + path, texture(texture, "_still"), texture(texture, "_flow"), color);
        setUnlocalizedName(Psychedelicraft.MOD_ID + "." + path);
        setViscosity(viscosity * 1000);
        this.ticks = ticks;
        this.distilledColor = distilledColor;
        this.matureColor = matureColor;
        this.fermentationAlcohol = fermentationAlcohol;
        this.distillationAlcohol = distillationAlcohol;
        this.maturationAlcohol = maturationAlcohol;
        this.styles = styles;
    }

    public FluidTicks getTicks() {
        return ticks.get();
    }

    public static int stage(FluidStack fluid, String name, int maximum) {
        return fluid.tag == null ? 0 : MathHelper.clamp(fluid.tag.getInteger(name), 0, maximum);
    }

    public static boolean isVinegar(FluidStack fluid) {
        return fluid.tag != null && fluid.tag.getBoolean(VINEGAR);
    }

    @Override
    public String getLocalizedName(FluidStack stack) {
        Style style = style(stack);
        String base = I18n.translateToLocal(getUnlocalizedName(stack));
        String name = I18n.translateToLocalFormatted("psychedelicraft.alcohol.drink." + style.name, base);
        return style.variant == null ? name
                : I18n.translateToLocalFormatted("psychedelicraft.alcohol.drink.variant." + style.variant, name);
    }

    @Override
    public ResourceLocation getStill(FluidStack stack) {
        return style(stack).still;
    }

    @Override
    public ResourceLocation getFlowing(FluidStack stack) {
        return style(stack).flowing;
    }

    @Override
    public int getColor(FluidStack stack) {
        int distillation = stage(stack, DISTILLATION, 16);
        int maturation = stage(stack, MATURATION, MATURATION_STEPS);
        int distilled = blend(super.getColor(stack), distilledColor, 1 - 1F / (1 + distillation));
        return blend(distilled, matureColor, 1 - 1F / (1 + maturation * 0.2F));
    }

    public ResourceLocation getSymbol(FluidStack stack) {
        return style(stack).symbol;
    }

    void addServingInfluences(FluidStack stack, double buckets, DrugProperties properties) {
        State state = new State(stack);
        Style style = style(state);
        if (!state.vinegar && !style.name.endsWith("vinegar")) {
            double alcohol = fermentationAlcohol * state.fermentation / FERMENTATION_STEPS
                    + distillationAlcohol * (1 - 1D / (1 + state.distillation))
                    + maturationAlcohol * (1 - 1D / (1 + state.maturation * 0.2));
            if (alcohol > 0) {
                properties.addInfluence(new DrugInfluence(DrugType.ALCOHOL, 20, 0.003, 0.002, alcohol * buckets));
            }
        }
        if ("ketchup".equals(style.name)) {
            properties.addInfluence(new DrugInfluence(DrugType.SUGAR, 20, 0.003, 0.002, 0.3 * buckets));
        }
    }

    public static void appendAttributes(FluidStack stack, List<String> tooltip) {
        State state = new State(stack);
        if (state.distillation > 0) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(
                    "psychedelicraft.alcohol.distillations", state.distillation));
        }
        if (state.fermentation > 0) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(
                    "psychedelicraft.alcohol.fermentations", state.fermentation));
        }
        if (state.maturation > 0) {
            String[] maturity = {"young", "aged", "mature", "very_mature", "very_mature"};
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted("psychedelicraft.alcohol.maturations",
                    state.maturation, I18n.translateToLocal("psychedelicraft.alcohol.maturity." + maturity[state.maturation / 4])));
        }
    }

    private Style style(FluidStack stack) {
        return style(new State(stack));
    }

    private Style style(State state) {
        for (Style style : styles) {
            if (style.matches.test(state)) return style;
        }
        return styles[0];
    }

    static int blend(int start, int end, float progress) {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int first = start >>> shift & 255;
            int last = end >>> shift & 255;
            result |= (int) (first + (last - first) * progress) << shift;
        }
        return result;
    }

    public static ResourceLocation texture(String appearance, String suffix) {
        return Psychedelicraft.id("blocks/fluid/" + appearance + suffix);
    }

    static final class State {
        final int fermentation;
        final int distillation;
        final int maturation;
        final boolean vinegar;

        State(FluidStack stack) {
            fermentation = stage(stack, FERMENTATION, FERMENTATION_STEPS);
            distillation = stage(stack, DISTILLATION, 16);
            maturation = stage(stack, MATURATION, MATURATION_STEPS);
            vinegar = isVinegar(stack);
        }
    }

    static final class Style {
        final String name;
        final String variant;
        final ResourceLocation still;
        final ResourceLocation flowing;
        final ResourceLocation symbol;
        final Predicate<State> matches;

        Style(String name, @Nullable String variant, String appearance, String symbol, Predicate<State> matches) {
            this.name = name;
            this.variant = variant;
            this.still = texture(appearance, "_still");
            this.flowing = texture(appearance, "_flow");
            this.symbol = Psychedelicraft.id("fluid/" + symbol);
            this.matches = matches;
        }
    }
}
