/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug;

import javax.annotation.Nullable;

import ivorius.psychedelicraft.util.NbtSerialisable;
import net.minecraft.entity.player.EntityPlayer;

public interface Drug extends NbtSerialisable {
    void update();

    default void update(DrugProperties properties, @Nullable EntityPlayer player) {
        update();
    }

    void reset();

    default void onWakeUp(DrugProperties properties, @Nullable EntityPlayer player) {
        reset();
    }

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

    default float superSaturationHallucinationStrength() {
        return 0;
    }

    default float viewWobblyness() {
        return 0;
    }

    default float hungerSuppression() {
        return 0;
    }

    default float speedModifier() {
        return 1;
    }

    default float digSpeedModifier() {
        return 1;
    }

    @Nullable
    default String sleepWarning() {
        return null;
    }

    @FunctionalInterface
    interface Modifier {
        float get(Drug drug);
    }
}
