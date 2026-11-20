/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 14 Jan 2023.
 */

package ivorius.psychedelicraft.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;

public final class HallucinationManager {
    private static final Effect[] EFFECTS = Effect.values();
    private static final float BROWN_SHROOM_WAVE_THRESHOLD = 0.15F;
    private final List<Effect> active = new ArrayList<>();
    private final float[] categoryStrengths = new float[Category.values().length];
    private final float[] strengths = new float[EFFECTS.length];
    private final float[] mindColor = {1F, 1F, 1F};

    public void update(DrugProperties properties, RandomSource random, int age) {
        float total = 0;
        for (Category category : Category.values()) {
            int index = category.ordinal();
            float desired = properties.getModifier(category.modifier);
            categoryStrengths[index] = nearValue(categoryStrengths[index], desired, 0.01F);
            total += categoryStrengths[index];
        }
        int count = Math.max(0, Mth.floor(total * 4F + 0.9F));

        // One mushroom peaks near 0.38. Reserve its small waves before the peak so they can build naturally.
        int reserved = count > 0 && properties.getActiveValue(DrugType.BROWN_SHROOMS) >= BROWN_SHROOM_WAVE_THRESHOLD ? 1 : 0;
        if (reserved > 0 && (active.isEmpty() || active.get(0) != Effect.SMALL_WAVES)) {
            active.remove(Effect.SMALL_WAVES);
            active.add(0, Effect.SMALL_WAVES);
        }

        while (active.size() > reserved && random.nextFloat() < 1F / (20 * 60 * 5 / active.size())) {
            removeRandom(random, reserved);
            addRandom(random);
        }
        while (active.size() > count) {
            removeRandom(random, reserved);
        }
        while (active.size() < count && addRandom(random)) {
            // Fill the remaining slots from the weighted effect pool.
        }

        float sway = (1F + Mth.sin(age * 0.00121F) * 0.5F)
                * (1F + Mth.sin(age * 0.0019318F) * 0.5F);
        for (Effect effect : EFFECTS) {
            float desired = active.contains(effect) ? multiplier(effect) * sway : 0F;
            int index = effect.ordinal();
            strengths[index] = nearValue(strengths[index], desired, 0.002F);
        }
        mindColor[0] = nearValue(mindColor[0], colorWave(age, 0.0012371F, 0.0017412F), 0.002F);
        mindColor[1] = nearValue(mindColor[1], colorWave(age, 0.0011239F, 0.0019321F), 0.002F);
        mindColor[2] = nearValue(mindColor[2], colorWave(age, 0.0011541F, 0.0018682F), 0.002F);
    }

    public float strength(Effect effect) {
        return multiplier(effect) * strengths[effect.ordinal()];
    }

    @Nullable
    public float[] bloomColor(@Nullable HarmoniumDrug harmonium) {
        return mixMindColor(harmonium, 3F, Mth.clamp(strength(Effect.COLOR_BLOOM) * 1.5F, 0F, 1F), false);
    }

    @Nullable
    public float[] contrastColor(@Nullable HarmoniumDrug harmonium) {
        return mixMindColor(harmonium, 1F, Mth.clamp(strength(Effect.COLOR_CONTRAST), 0F, 1F), true);
    }

    @Nullable
    public float[] pulseColor() {
        float strength = Mth.clamp(strength(Effect.PULSES), 0F, 1F);
        return strength > 0F ? new float[]{mindColor[0], mindColor[1], mindColor[2], strength} : null;
    }

    @Nullable
    private float[] mixMindColor(@Nullable HarmoniumDrug harmonium, float multiplier, float randomStrength, boolean clampAlpha) {
        float dyeStrength = harmonium == null ? 0F : (float) harmonium.getActiveValue() * multiplier;
        float strength = dyeStrength + randomStrength;
        if (strength <= 0F) return null;
        float[] result = {mindColor[0], mindColor[1], mindColor[2], clampAlpha ? Math.min(1F, strength) : strength};
        if (dyeStrength > 0F) {
            float[] dye = harmonium.color();
            float blend = randomStrength / strength;
            for (int channel = 0; channel < 3; channel++) {
                result[channel] = dye[channel] + (mindColor[channel] - dye[channel]) * blend;
            }
        }
        return result;
    }

    private static float colorWave(int age, float first, float second) {
        return 0.5F * (1F + Mth.sin(age * first) * 0.5F) * (1F + Mth.sin(age * second) * 0.5F);
    }

    private void removeRandom(RandomSource random, int reserved) {
        active.remove(reserved + random.nextInt(active.size() - reserved));
    }

    private boolean addRandom(RandomSource random) {
        float bestScore = 0;
        Effect best = null;
        for (Effect candidate : EFFECTS) {
            if (!active.contains(candidate)) {
                float score = random.nextFloat() * multiplier(candidate);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        if (best == null) {
            return false;
        }
        active.add(best);
        return true;
    }

    private float multiplier(Effect effect) {
        return Mth.clamp(categoryStrengths[effect.category.ordinal()] / 0.5F, 0, 1);
    }

    private static float nearValue(float current, float desired, float rate) {
        float value = current + rate * (desired - current);
        return value > desired ? Math.max(desired, value - rate) : Math.min(desired, value + rate);
    }

    private enum Category {
        COLOR(Drug::colorHallucinationStrength),
        MOVEMENT(Drug::movementHallucinationStrength),
        CONTEXTUAL(Drug::contextualHallucinationStrength);

        private final Drug.Modifier modifier;

        Category(Drug.Modifier modifier) {
            this.modifier = modifier;
        }
    }

    public enum Effect {
        ENTITIES(Category.CONTEXTUAL),
        DESATURATION(Category.COLOR),
        SUPER_SATURATION(Category.COLOR),
        SLOW_COLOR_ROTATION(Category.COLOR),
        QUICK_COLOR_ROTATION(Category.COLOR),
        BIG_WAVES(Category.MOVEMENT),
        SMALL_WAVES(Category.MOVEMENT),
        WIGGLE_WAVES(Category.MOVEMENT),
        PULSES(Category.COLOR),
        SURFACE_FRACTALS(Category.COLOR),
        DISTANT_WORLD_DEFORMATION(Category.MOVEMENT),
        BLOOM(Category.COLOR),
        COLOR_BLOOM(Category.COLOR),
        COLOR_CONTRAST(Category.COLOR);

        private final Category category;

        Effect(Category category) {
            this.category = category;
        }
    }
}
