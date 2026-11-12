/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

public final class TobaccoDrug extends SimpleDrug {
    public TobaccoDrug() {
        super(1, 0.003);
    }

    @Override
    public float desaturationHallucinationStrength() {
        return (float) getActiveValue() * 0.2F;
    }
}
