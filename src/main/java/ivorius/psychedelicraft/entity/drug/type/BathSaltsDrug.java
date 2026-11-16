/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Created by Sollace on 6 Feb 2023.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;

public final class BathSaltsDrug extends SimpleDrug {
    public BathSaltsDrug() {
        super(1, 0.00012);
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
    public float inversionStrength() {
        float strength = (float) getActiveValue();
        return Mth.clamp(strength * strength * 5.3F, 0F, 1.5F);
    }

    @Override
    public float bloomStrength() {
        return (float) getActiveValue() * 0.12F;
    }

    @Override
    public void update(DrugProperties properties, @Nullable Player player) {
        super.update(properties, player);
        if (player == null || player.level().isClientSide || getActiveValue() <= 0.8 || player.tickCount % 20 != 0) return;
        RandomSource random = player.getRandom();
        if (random.nextFloat() < (getActiveValue() - 0.8F) * 0.051) {
            if (random.nextFloat() < 0.4F) player.hurt(PSDamageTypes.of(player, PSDamageTypes.STROKE), Integer.MAX_VALUE);
            else if (random.nextBoolean()) player.hurt(PSDamageTypes.of(player, PSDamageTypes.HEART_FAILURE), Integer.MAX_VALUE);
            else if (random.nextBoolean()) player.hurt(PSDamageTypes.of(player, PSDamageTypes.RESPIRATORY_FAILURE), Integer.MAX_VALUE);
            else if (random.nextBoolean()) player.hurt(PSDamageTypes.of(player, PSDamageTypes.KIDNEY_FAILURE), Integer.MAX_VALUE);
        }
    }

    @Override
    public void onWakeUp(DrugProperties properties, @Nullable Player player) {
        if (player != null && !player.level().isClientSide && getActiveValue() > 0) {
            RandomSource random = player.getRandom();
            if (random.nextBoolean()) {
                player.hurt(random.nextFloat() < 0.002F ? PSDamageTypes.of(player, PSDamageTypes.KIDNEY_FAILURE) : PSDamageTypes.of(player, PSDamageTypes.IN_SLEEP),
                        Integer.MAX_VALUE);
                return;
            }
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0, false, false));
        }
        reset();
    }

    @Override
    public float colorHallucinationStrength() {
        return (float) getActiveValue() * 0.8F;
    }

    @Override
    public float movementHallucinationStrength() {
        return (float) getActiveValue();
    }
}
