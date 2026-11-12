/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import net.minecraft.util.math.MathHelper;

public final class CaffeineDrug extends SimpleDrug {
    public CaffeineDrug(double decreaseSpeed, double decreaseSpeedPlus) {
        super(decreaseSpeed, decreaseSpeedPlus);
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
