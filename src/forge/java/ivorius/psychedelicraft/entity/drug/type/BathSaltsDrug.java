/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Created by Sollace on 6 Feb 2023.
 */

package ivorius.psychedelicraft.entity.drug.type;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.Random;

public final class BathSaltsDrug extends SimpleDrug {
    public BathSaltsDrug() {
        super(1, 0.00012);
    }

    @Override
    public float inversionStrength() {
        float strength = (float) getActiveValue();
        return MathHelper.clamp(strength * strength * 5.3F, 0F, 1.5F);
    }

    @Override
    public float bloomStrength() {
        return (float) getActiveValue() * 0.12F;
    }

    @Override
    public void update(DrugProperties properties, @Nullable EntityPlayer player) {
        super.update(properties, player);
        if (player == null || player.world.isRemote || getActiveValue() <= 0.8 || player.ticksExisted % 20 != 0) return;
        Random random = player.getRNG();
        if (random.nextFloat() < (getActiveValue() - 0.8F) * 0.051) {
            if (random.nextFloat() < 0.4F) player.attackEntityFrom(PSDamageTypes.STROKE, Integer.MAX_VALUE);
            else if (random.nextBoolean()) player.attackEntityFrom(PSDamageTypes.HEART_FAILURE, Integer.MAX_VALUE);
            else if (random.nextBoolean()) player.attackEntityFrom(PSDamageTypes.RESPIRATORY_FAILURE, Integer.MAX_VALUE);
            else if (random.nextBoolean()) player.attackEntityFrom(PSDamageTypes.KIDNEY_FAILURE, Integer.MAX_VALUE);
        }
    }

    @Override
    public void onWakeUp(DrugProperties properties, @Nullable EntityPlayer player) {
        if (player != null && !player.world.isRemote && getActiveValue() > 0) {
            Random random = player.getRNG();
            if (random.nextBoolean()) {
                player.attackEntityFrom(random.nextFloat() < 0.002F ? PSDamageTypes.KIDNEY_FAILURE : PSDamageTypes.IN_SLEEP,
                        Integer.MAX_VALUE);
                return;
            }
            player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 300, 0, false, false));
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
