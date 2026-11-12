/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

public final class CannabisDrug extends SimpleDrug {
    public CannabisDrug() {
        super(1, 0.0002);
    }

    @Override
    public void update(DrugProperties properties, @Nullable EntityPlayer player) {
        super.update();
        if (player != null && !player.world.isRemote && getActiveValue() > 0) {
            player.addExhaustion(0.03F * (float) getActiveValue());
        }
    }

    @Override
    public float speedModifier() {
        return 1 - (float) getActiveValue() * 0.5F;
    }

    @Override
    public float digSpeedModifier() {
        return speedModifier();
    }

    @Override
    public float superSaturationHallucinationStrength() {
        return MathHelper.clamp((float) getActiveValue() / 0.5F, 0, 1) * 0.3F;
    }

    @Override
    public float colorHallucinationStrength() {
        return MathHelper.clamp(((float) getActiveValue() * 1.3F - 0.5F) / 0.5F, 0, 1) * 0.1F;
    }

    @Override
    public float movementHallucinationStrength() {
        return colorHallucinationStrength();
    }

    @Override
    public float contextualHallucinationStrength() {
        return colorHallucinationStrength();
    }

    @Override
    public float viewWobblyness() {
        return (float) getActiveValue() * 0.02F;
    }

    @Override
    public float hungerSuppression() {
        return (float) getActiveValue() * -0.2F;
    }
}
