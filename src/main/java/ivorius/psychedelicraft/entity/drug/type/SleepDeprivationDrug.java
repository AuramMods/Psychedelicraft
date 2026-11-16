/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Created by Sollace on 19 Apr 2023.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public final class SleepDeprivationDrug extends SimpleDrug {
    public static final String GAME_RULE = "doSleepDeprivation";

    private float storedEnergy;

    public SleepDeprivationDrug() {
        super(1, 0);
    }

    @Override
    public void update(DrugProperties properties, @Nullable Player player) {
        super.update(properties, player);
        float stimulants = (float) (properties.getActiveValue(DrugType.CAFFEINE) + properties.getActiveValue(DrugType.COCAINE));
        float target = Math.min(1, stimulants * 10);
        storedEnergy += Mth.clamp(target - storedEnergy, -0.02F, 0.02F);
        if (stimulants > 0.1F) {
            scaleDesiredValue(0);
        } else if (player != null && player.level().getGameRules().getBoolean(ivorius.psychedelicraft.Psychedelicraft.SLEEP_DEPRIVATION)) {
            addToDesiredValue(1D / 216000);
        }
    }

    @Override
    public double getActiveValue() {
        return Math.max(0, super.getActiveValue() - storedEnergy);
    }

    @Override
    public float drowsiness() {
        return (float) getActiveValue();
    }

    @Override
    public float speedModifier() {
        return 1 - Math.max(0, (float) getActiveValue() * 0.9F - 0.4F);
    }

    @Override
    public float digSpeedModifier() {
        return speedModifier();
    }

    @Override
    public float motionBlur() {
        return Math.max(0, (float) getActiveValue() - 0.6F) * 3;
    }

    @Override
    public float desaturationHallucinationStrength() {
        return Math.max(0, ((float) getActiveValue() - 0.5F) * 2);
    }

    @Override
    public float soundVolumeModifier() {
        return 1 + desaturationHallucinationStrength();
    }

    @Override
    public float movementHallucinationStrength() {
        return Math.max(0, (float) getActiveValue() - 0.8F) * 3;
    }

    @Override
    public float contextualHallucinationStrength() {
        return Math.max(0, (float) getActiveValue() - 0.8F) * 4;
    }

    @Override
    public void onWakeUp(DrugProperties properties, @Nullable Player player) {
        clearActiveValue();
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        storedEnergy = 0;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        tag.putFloat("storedEnergy", storedEnergy);
    }

    @Override
    public void fromNbt(CompoundTag tag) {
        super.fromNbt(tag);
        float saved = tag.getFloat("storedEnergy");
        storedEnergy = Float.isFinite(saved) ? Mth.clamp(saved, 0, 1) : 0;
    }
}
