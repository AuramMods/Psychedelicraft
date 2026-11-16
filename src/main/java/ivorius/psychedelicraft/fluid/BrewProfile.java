/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.fluid;

import com.google.common.base.Suppliers;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig.FluidTicks;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BrewProfile extends FluidProfile {
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
    private final com.google.common.base.Supplier<List<CompoundTag>> creativeAttributes = Suppliers.memoize(this::findCreativeAttributes);

    BrewProfile(String path, String texture, int color, int viscosity, Supplier<FluidTicks> ticks,
              int distilledColor, int matureColor, double fermentationAlcohol,
              double distillationAlcohol, double maturationAlcohol, Style... styles) {
        super(path, texture, color, viscosity);
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
        return fluid.getTag() == null ? 0 : Mth.clamp(fluid.getTag().getInt(name), 0, maximum);
    }

    public static boolean isVinegar(FluidStack fluid) {
        return fluid.getTag() != null && fluid.getTag().getBoolean(VINEGAR);
    }

    @Override public Component getDisplayName(FluidStack stack) {
        Style style=style(stack);
        Component name=Component.translatable("psychedelicraft.alcohol.drink."+style.name,super.getDisplayName(stack));
        return style.variant==null?name:Component.translatable("psychedelicraft.alcohol.drink.variant."+style.variant,name);
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

    public boolean isDrinkType(FluidStack stack, String drink) {
        return style(stack).name.equals(drink);
    }

    void addServingInfluences(FluidStack stack, double buckets, DrugProperties properties) {
        State state = new State(stack);
        Style style = style(state);
        if (!state.vinegar && !style.name.endsWith("vinegar")) {
            double alcohol = alcoholPerBucket(state);
            if (alcohol > 0) {
                properties.addInfluence(new DrugInfluence(DrugType.ALCOHOL, 20, 0.003, 0.002, alcohol * buckets));
            }
        }
        if ("ketchup".equals(style.name)) {
            properties.addInfluence(new DrugInfluence(DrugType.SUGAR, 20, 0.003, 0.002, 0.3 * buckets));
        }
    }

    private double alcoholPerBucket(State state) {
        return fermentationAlcohol * state.fermentation / FERMENTATION_STEPS
                + distillationAlcohol * (1 - 1D / (1 + state.distillation))
                + maturationAlcohol * (1 - 1D / (1 + state.maturation * 0.2));
    }

    public static float projectileStrength(@Nullable FluidStack contents) {
        if (contents == null || PSFluids.brew(contents.getFluid()) == null) return 0;
        BrewProfile brew = PSFluids.brew(contents.getFluid());
        float buckets = contents.getAmount() / 1000F;
        return Mth.clamp((float) brew.alcoholPerBucket(new State(contents)) * buckets, 0, 1) * buckets;
    }

    public void addCreativeVariants(int amount, boolean shotGlass, Consumer<FluidStack> add) {
        for (CompoundTag attributes : creativeAttributes.get()) {
            FluidStack sample = new FluidStack(PSFluids.resolve(Psychedelicraft.id(path).toString()), amount, attributes.isEmpty() ? null : attributes.copy());
            if (!path.equals("agave") || shotGlass == "tequila".equals(style(sample).name)) add.accept(sample);
        }
    }

    private List<CompoundTag> findCreativeAttributes() {
        List<CompoundTag> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // The donor advances its state index in steps of 16 while each stage has 17 values.
        int stageStride = MATURATION_STEPS;
        int period = 2 * stageStride * stageStride * (FERMENTATION_STEPS + 1) * (stageStride + 1);
        for (int index = 0; index < period; index++) {
            int stages = index / 2;
            State state = new State(stages / (stageStride * stageStride) % (FERMENTATION_STEPS + 1),
                    stages % (stageStride + 1), stages / stageStride % (stageStride + 1), index % 2 != 0);
            Style style = style(state);
            if (seen.add(style.name + "/" + style.variant)) {
                CompoundTag tag = new CompoundTag();
                if (state.fermentation != 0) tag.putInt(FERMENTATION, state.fermentation);
                if (state.distillation != 0) tag.putInt(DISTILLATION, state.distillation);
                if (state.maturation != 0) tag.putInt(MATURATION, state.maturation);
                if (state.vinegar) tag.putBoolean(VINEGAR, true);
                result.add(tag);
            }
        }
        return result;
    }

    public static void appendAttributes(FluidStack stack,List<Component> tooltip){
        State state=new State(stack);
        if(state.distillation>0)tooltip.add(Component.translatable("psychedelicraft.alcohol.distillations",state.distillation).withStyle(ChatFormatting.GRAY));
        if(state.fermentation>0)tooltip.add(Component.translatable("psychedelicraft.alcohol.fermentations",state.fermentation).withStyle(ChatFormatting.GRAY));
        if(state.maturation>0){
            String[] maturity={"young","aged","mature","very_mature","very_mature"};
            tooltip.add(Component.translatable("psychedelicraft.alcohol.maturations",state.maturation,Component.translatable("psychedelicraft.alcohol.maturity."+maturity[state.maturation/4])).withStyle(ChatFormatting.GRAY));
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
        return Psychedelicraft.id("block/fluid/" + appearance + suffix);
    }

    static final class State {
        final int fermentation;
        final int distillation;
        final int maturation;
        final boolean vinegar;

        State(FluidStack stack) {
            this(stage(stack, FERMENTATION, FERMENTATION_STEPS), stage(stack, DISTILLATION, 16),
                    stage(stack, MATURATION, MATURATION_STEPS), isVinegar(stack));
        }

        private State(int fermentation, int distillation, int maturation, boolean vinegar) {
            this.fermentation = fermentation;
            this.distillation = distillation;
            this.maturation = maturation;
            this.vinegar = vinegar;
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
