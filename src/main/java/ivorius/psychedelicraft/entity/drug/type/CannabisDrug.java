/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public final class CannabisDrug extends SimpleDrug {
    public CannabisDrug() {
        super(1, 0.0002);
    }

    @Override
    public void update(DrugProperties properties, @Nullable Player player) {
        super.update(properties, player);
        if (player != null && !player.level().isClientSide && getActiveValue() > 0) {
            player.causeFoodExhaustion(0.03F * (float) getActiveValue());
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
        return Mth.clamp((float) getActiveValue() / 0.5F, 0, 1) * 0.3F;
    }

    @Override
    public float colorHallucinationStrength() {
        return Mth.clamp(((float) getActiveValue() * 1.3F - 0.5F) / 0.5F, 0, 1) * 0.1F;
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
    public float headMotionInertia() {
        return (float) getActiveValue() * 8F;
    }

    @Override
    public float hungerSuppression() {
        return (float) getActiveValue() * -0.2F;
    }
}
