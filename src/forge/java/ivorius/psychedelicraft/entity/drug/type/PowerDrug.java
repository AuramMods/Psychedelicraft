/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 4 Jan 2023.
 */

package ivorius.psychedelicraft.entity.drug.type;

public final class PowerDrug extends SimpleDrug {
    public PowerDrug() {
        super(0.95, 0.0001);
    }

    @Override
    public float desaturationHallucinationStrength() {
        return (float) getActiveValue() * 0.75F;
    }

    @Override
    public float motionBlur() {
        return (float) getActiveValue() * 0.3F;
    }
}
