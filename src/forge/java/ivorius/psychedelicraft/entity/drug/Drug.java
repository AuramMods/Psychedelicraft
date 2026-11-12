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

    void setDesiredValue(double value);

    void setLocked(boolean locked);

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

    default float desaturationHallucinationStrength() {
        return 0;
    }

    default float inversionStrength() {
        return 0;
    }

    default float bloomStrength() {
        return 0;
    }

    default float doubleVision() {
        return 0;
    }

    default float motionBlur() {
        return 0;
    }

    default float soundVolumeModifier() {
        return 1;
    }

    default float viewWobblyness() {
        return 0;
    }

    default float weightlessness() {
        return 0;
    }

    default float headMotionInertia() {
        return 0;
    }

    default float viewTrembleStrength() {
        return 0;
    }

    default float handTrembleStrength() {
        return 0;
    }

    default float hungerSuppression() {
        return 0;
    }

    default float heartbeatVolume() {
        return 0;
    }

    default float heartbeatSpeed() {
        return 0;
    }

    default float breathVolume() {
        return 0;
    }

    default float breathSpeed() {
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
