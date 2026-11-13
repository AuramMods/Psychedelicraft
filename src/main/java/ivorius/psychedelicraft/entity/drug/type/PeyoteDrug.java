/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

public final class PeyoteDrug extends SimpleDrug {
    public PeyoteDrug(double decreaseSpeed, double decreaseSpeedPlus) {
        super(decreaseSpeed, decreaseSpeedPlus);
    }

    @Override
    public float colorHallucinationStrength() {
        return (float) getActiveValue() * 0.3F;
    }

    @Override
    public float contextualHallucinationStrength() {
        return (float) getActiveValue() * 0.6F;
    }
}
