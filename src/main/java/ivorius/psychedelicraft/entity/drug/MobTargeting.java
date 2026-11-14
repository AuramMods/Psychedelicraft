package ivorius.psychedelicraft.entity.drug;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;

public final class MobTargeting {
    private MobTargeting() {
    }

    @Nullable
    public static EntityLivingBase acceptedTarget(EntityLiving mob) {
        return mob.getAttackTarget();
    }

    @Nullable
    public static EntityLivingBase filterTarget(EntityLiving mob, @Nullable EntityLivingBase target) {
        if (mob.world.isRemote || !(target instanceof EntityPlayer)) return target;
        DrugProperties drugs = DrugCapability.get((EntityPlayer) target);
        if (drugs == null) return target;
        float chance = drugs.getModifier(Drug::mobAvoidanceChance);
        if (chance >= 1F || (chance > 0F && mob.getAttackTarget() != target && mob.getRNG().nextFloat() < chance)) return null;
        return target;
    }
}
