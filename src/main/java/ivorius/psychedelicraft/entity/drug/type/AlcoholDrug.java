/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;

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
    public void update(DrugProperties properties, @Nullable Player player) {
        super.update(properties, player);
        if (player == null || getActiveValue() <= 0) return;
        double active = getActiveValue();
        int age = player.tickCount;
        RandomSource random = player.getRandom();
        if (!player.level().isClientSide) {
            if (age % 20 == 0 && random.nextFloat() < (active - 0.9F) * 2) {
                player.hurt(PSDamageTypes.of(player, PSDamageTypes.ALCOHOL_POISONING), (int) ((active - 0.9F) * 50 + 4));
            }
        } else if (player.isLocalPlayer()) {
            double drift = Math.min(active, 0.8);
            player.setXRot(Mth.clamp(player.getXRot()
                    + (float) drift * Mth.sin(age / 600F * (float) Math.PI) / 2 * (random.nextFloat() + 0.5F)
                    + (float) drift * Mth.sin(age / 180F * (float) Math.PI) / 3 * (random.nextFloat() + 0.5F),
                    -90, 90));
            player.setYRot(player.getYRot() + (float) drift * Mth.cos(age / 500F * (float) Math.PI) / 1.3F * (random.nextFloat() + 0.5F)
                    + (float) drift * Mth.cos(age / 150F * (float) Math.PI) / 2 * (random.nextFloat() + 0.5F));
        }
    }

    @Override
    public void onWakeUp(DrugProperties properties, @Nullable Player player) {
        double strength = getActiveValue();
        reset();
        if (player != null && !player.level().isClientSide && strength > 0 && player.getRandom().nextFloat() > 1 - strength) {
            player.level().broadcastEntityEvent(player, (byte) 2);
            properties.addToDrug(DrugType.SLEEP_DEPRIVATION, 0.25);
            PSCriteria.hangover(player);
        }
    }
}
