/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.StomachFoodStats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.Random;

public final class CocaineDrug extends SimpleDrug {
    public CocaineDrug() {
        super(1, 0.0003);
    }

    @Override
    public float heartbeatVolume() {
        return activeRamp(0.4F) + ticksActive() * 0.0001F * 1.2F;
    }

    @Override
    public float heartbeatSpeed() {
        return (float) getActiveValue() * 0.1F + ticksActive() * 0.0001F;
    }

    @Override
    public float breathVolume() {
        return activeRamp(0.4F) * 1.5F;
    }

    @Override
    public float breathSpeed() {
        return (float) getActiveValue() * 0.8F;
    }

    @Override
    public float randomJumpChance() {
        return activeRamp(0.6F) * 0.03F;
    }

    @Override
    public float randomSwingChance() {
        return activeRamp(0.5F) * 0.02F;
    }

    @Override
    public float handTrembleStrength() {
        return activeRamp(0.6F);
    }

    @Override
    public float viewTrembleStrength() {
        return activeRamp(0.8F);
    }

    @Override
    public float headMotionInertia() {
        return (float) getActiveValue() * 10F;
    }

    @Override
    public float desaturationHallucinationStrength() {
        return (float) getActiveValue() * 0.75F;
    }

    @Override
    public float bloomStrength() {
        return Math.min(1F, (float) getActiveValue() / 0.6F) * 1.5F;
    }

    @Override
    public void update(DrugProperties properties, @Nullable EntityPlayer player) {
        super.update(properties, player);
        if (player == null || player.world.isRemote || getActiveValue() <= 0 || player.ticksExisted % 20 != 0) return;
        StomachFoodStats.restoreSaturation(player, 1F);
        if (getActiveValue() <= 0.8) return;
        Random random = player.getRNG();
        if (random.nextFloat() < (getActiveValue() - 0.8F) * 0.1) {
            DamageSource cause;
            if (random.nextFloat() < 0.4F) cause = PSDamageTypes.STROKE;
            else if (random.nextBoolean()) cause = PSDamageTypes.HEART_FAILURE;
            else cause = PSDamageTypes.RESPIRATORY_FAILURE;
            player.attackEntityFrom(cause, Integer.MAX_VALUE);
        }
    }

    @Override
    public float speedModifier() {
        return 1 + (float) getActiveValue() * 0.15F;
    }

    @Override
    public float digSpeedModifier() {
        return 1F + (float) getActiveValue();
    }

    @Override
    public float attackDamageModifier() {
        return 1F + 0.15F * (float) getActiveValue();
    }

    @Override
    @Nullable
    public String sleepWarning() {
        return getActiveValue() > 0.4 ? "psychedelicraft.sleep.fail.coccaine" : null;
    }

    @Override
    public float colorHallucinationStrength() {
        return MathHelper.clamp(((float) getActiveValue() * 1.3F - 0.7F) / 0.3F, 0, 1) * 0.05F;
    }

    @Override
    public float movementHallucinationStrength() {
        return colorHallucinationStrength();
    }

    @Override
    public float contextualHallucinationStrength() {
        return colorHallucinationStrength();
    }
}
