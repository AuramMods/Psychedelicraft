/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

public final class CaffeineDrug extends SimpleDrug {
    private final float breathMultiplier;

    public CaffeineDrug(double decreaseSpeed, double decreaseSpeedPlus) {
        this(decreaseSpeed, decreaseSpeedPlus, 1F);
    }

    public CaffeineDrug(double decreaseSpeed, double decreaseSpeedPlus, float breathMultiplier) {
        super(decreaseSpeed, decreaseSpeedPlus);
        this.breathMultiplier = breathMultiplier;
    }

    @Override
    public float heartbeatVolume() {
        return breathMultiplier * activeRamp(0.6F) + ticksActive() * 0.001F;
    }

    @Override
    public float heartbeatSpeed() {
        return breathMultiplier * (float) getActiveValue() * 0.2F + ticksActive() * 0.001F;
    }

    @Override
    public float breathVolume() {
        return breathMultiplier * activeRamp(0.4F) * 0.5F;
    }

    @Override
    public float breathSpeed() {
        return breathMultiplier * (float) getActiveValue() * 0.3F;
    }

    @Override
    public float randomJumpChance() {
        return activeRamp(0.6F) * 0.07F;
    }

    @Override
    public float randomSwingChance() {
        return activeRamp(0.3F) * 0.05F;
    }

    @Override
    public float handTrembleStrength() {
        return activeRamp(0.6F);
    }

    @Override
    public float viewTrembleStrength() {
        return activeRamp(0.8F);
    }

    @Override
    public float speedModifier() {
        return 1 + (float) getActiveValue() * 0.2F;
    }

    @Override
    public float digSpeedModifier() {
        return speedModifier();
    }

    @Override
    @Nullable
    public String sleepWarning() {
        return getActiveValue() > 0.1 ? "psychedelicraft.sleep.fail.caffiene" : null;
    }

    @Override
    public float superSaturationHallucinationStrength() {
        return (float) getActiveValue() * 0.3F;
    }

    @Override
    public float colorHallucinationStrength() {
        return hallucinationFactor() * 0.03F;
    }

    @Override
    public float movementHallucinationStrength() {
        return hallucinationFactor() * 0.03F;
    }

    @Override
    public float contextualHallucinationStrength() {
        return hallucinationFactor() * 0.05F;
    }

    @Override
    public float hungerSuppression() {
        return (float) getActiveValue() * 0.15F;
    }

    private float hallucinationFactor() {
        return MathHelper.clamp(((float) getActiveValue() * 1.3F - 0.7F) / (1F - 0.7F), 0, 1);
    }
}
