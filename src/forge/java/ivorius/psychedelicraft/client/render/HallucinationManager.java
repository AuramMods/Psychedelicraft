/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 14 Jan 2023.
 */

package ivorius.psychedelicraft.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.util.math.MathHelper;

public final class HallucinationManager {
    private static final Effect[] EFFECTS = Effect.values();
    private final List<Effect> active = new ArrayList<>();
    private final float[] categoryStrengths = new float[Category.values().length];
    private final float[] strengths = new float[EFFECTS.length];

    public void update(DrugProperties properties, Random random, int age) {
        float total = 0;
        for (Category category : Category.values()) {
            int index = category.ordinal();
            float desired = properties.getModifier(category.modifier);
            categoryStrengths[index] = nearValue(categoryStrengths[index], desired, 0.01F);
            total += categoryStrengths[index];
        }
        int count = Math.max(0, MathHelper.floor(total * 4F + 0.9F));

        while (!active.isEmpty() && random.nextFloat() < 1F / (20 * 60 * 5 / active.size())) {
            active.remove(random.nextInt(active.size()));
            addRandom(random);
        }
        while (active.size() > count) {
            active.remove(random.nextInt(active.size()));
        }
        while (active.size() < count && addRandom(random)) {
            // Fill the remaining slots from the weighted effect pool.
        }

        float sway = (1F + MathHelper.sin(age * 0.00121F) * 0.5F)
                * (1F + MathHelper.sin(age * 0.0019318F) * 0.5F);
        for (Effect effect : EFFECTS) {
            float desired = active.contains(effect) ? multiplier(effect) * sway : 0F;
            int index = effect.ordinal();
            strengths[index] = nearValue(strengths[index], desired, 0.002F);
        }
    }

    public float strength(Effect effect) {
        return multiplier(effect) * strengths[effect.ordinal()];
    }

    private boolean addRandom(Random random) {
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
        return MathHelper.clamp(categoryStrengths[effect.category.ordinal()] / 0.5F, 0, 1);
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
        SHATTERING_FRACTALS(Category.MOVEMENT),
        BLOOM(Category.COLOR),
        COLOR_BLOOM(Category.COLOR),
        COLOR_CONTRAST(Category.COLOR);

        private final Category category;

        Effect(Category category) {
            this.category = category;
        }
    }
}
