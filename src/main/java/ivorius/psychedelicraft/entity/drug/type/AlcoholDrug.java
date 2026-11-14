/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.Random;

public final class AlcoholDrug extends SimpleDrug {
    public AlcoholDrug() {
        super(1, 0.0002);
    }

    @Override
    public float viewWobblyness() {
        return (float) getActiveValue() * 0.5F;
    }

    @Override
    public float doubleVision() {
        return activeRamp(0.25F);
    }

    @Override
    public float motionBlur() {
        return activeRamp(0.5F) * 0.3F;
    }

    @Override
    public float incomingDamageModifier() {
        return 1F - 0.2F * (float) getActiveValue();
    }

    @Override
    public float saturationExhaustionModifier() {
        return 1F - 0.5F * (float) getActiveValue();
    }

    @Override
    public void update(DrugProperties properties, @Nullable EntityPlayer player) {
        super.update(properties, player);
        if (player == null || getActiveValue() <= 0) return;
        double active = getActiveValue();
        int age = player.ticksExisted;
        Random random = player.getRNG();
        if (!player.world.isRemote) {
            if (age % 20 == 0 && random.nextFloat() < (active - 0.9F) * 2) {
                player.attackEntityFrom(PSDamageTypes.ALCOHOL_POISONING, (int) ((active - 0.9F) * 50 + 4));
            }
        } else if (player.isUser()) {
            double drift = Math.min(active, 0.8);
            player.rotationPitch = MathHelper.clamp(player.rotationPitch
                    + (float) drift * MathHelper.sin(age / 600F * (float) Math.PI) / 2 * (random.nextFloat() + 0.5F)
                    + (float) drift * MathHelper.sin(age / 180F * (float) Math.PI) / 3 * (random.nextFloat() + 0.5F),
                    -90, 90);
            player.rotationYaw += (float) drift * MathHelper.cos(age / 500F * (float) Math.PI) / 1.3F * (random.nextFloat() + 0.5F)
                    + (float) drift * MathHelper.cos(age / 150F * (float) Math.PI) / 2 * (random.nextFloat() + 0.5F);
        }
    }

    @Override
    public void onWakeUp(DrugProperties properties, @Nullable EntityPlayer player) {
        double strength = getActiveValue();
        reset();
        if (player != null && !player.world.isRemote && strength > 0 && player.getRNG().nextFloat() > 1 - strength) {
            player.world.setEntityState(player, (byte) 2);
            properties.addToDrug(DrugType.SLEEP_DEPRIVATION, 0.25);
            PSCriteria.hangover(player);
        }
    }
}
