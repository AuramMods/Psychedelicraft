/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.util.NbtSerialisable;

public interface Drug extends NbtSerialisable {
    void update();

    void reset();

    double getActiveValue();

    void addToDesiredValue(double value);

    default float colorHallucinationStrength() {
        return 0;
    }

    default float movementHallucinationStrength() {
        return 0;
    }

    default float contextualHallucinationStrength() {
        return 0;
    }

    default float viewWobblyness() {
        return 0;
    }

    default float hungerSuppression() {
        return 0;
    }

    @FunctionalInterface
    interface Modifier {
        float get(Drug drug);
    }
}
