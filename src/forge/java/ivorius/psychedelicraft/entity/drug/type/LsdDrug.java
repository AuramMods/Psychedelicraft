/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Created by Sollace on Feb 6 2023.
 */

package ivorius.psychedelicraft.entity.drug.type;

import javax.annotation.Nullable;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.entity.player.EntityPlayer;

public final class LsdDrug extends SimpleDrug {
    private final boolean harmful;

    public LsdDrug(double decreaseSpeed, double decreaseSpeedPlus, boolean harmful) {
        super(decreaseSpeed, decreaseSpeedPlus);
        this.harmful = harmful;
    }

    @Override
    public void update(DrugProperties properties, @Nullable EntityPlayer player) {
        if (harmful && getActiveValue() >= 0.99F) {
            if (properties.getActiveValue(DrugType.CAFFEINE) > 0) {
                properties.addToDrug(DrugType.CAFFEINE, -0.5);
                scaleDesiredValue(0.5);
            } else if (player != null && !player.world.isRemote) {
                player.attackEntityFrom(PSDamageTypes.STROKE, 1);
            }
        }
        super.update();
    }

    @Override
    public float colorHallucinationStrength() {
        return (float) Math.max(0, getActiveValue() - 0.6F) * 2.8F;
    }

    @Override
    public float movementHallucinationStrength() {
        return (float) Math.max(0, getActiveValue() - 0.6F) * 1.9F;
    }

    @Override
    public float superSaturationHallucinationStrength() {
        return (float) getActiveValue() * 0.8F;
    }

    @Override
    public float hungerSuppression() {
        return (float) getActiveValue() * 0.2F;
    }

    @Override
    public float heartbeatVolume() {
        return harmful ? activeRamp(0.4F) + ticksActive() * 0.0001F * 1.2F : 0F;
    }

    @Override
    public float heartbeatSpeed() {
        return harmful ? (float) getActiveValue() * 0.1F + ticksActive() * 0.0001F : 0F;
    }
}
