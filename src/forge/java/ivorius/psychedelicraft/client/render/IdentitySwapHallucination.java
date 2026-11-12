package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;

import javax.annotation.Nullable;

final class IdentitySwapHallucination {
    private final HallucinationType type;
    private int age;
    private EntityLivingBase source;
    private EntityLivingBase disguise;

    IdentitySwapHallucination(HallucinationType type) {
        this.type = type;
    }

    boolean tick() {
        return ++age > 300 || source != null && source.isDead;
    }

    @Nullable
    Entity disguise(Entity original) {
        Class<?> target = type == HallucinationType.HOSTILE_VILLAGERS ? EntityVillager.class : EntityZombie.class;
        if (original.getClass() != target || source != null && source != original) return null;
        if (source == null) {
            source = (EntityLivingBase) original;
            disguise = createDisguise();
        }

        disguise.copyLocationAndAnglesFrom(source);
        disguise.lastTickPosX = source.lastTickPosX;
        disguise.lastTickPosY = source.lastTickPosY;
        disguise.lastTickPosZ = source.lastTickPosZ;
        disguise.prevRotationYaw = source.prevRotationYaw;
        disguise.prevRotationPitch = source.prevRotationPitch;
        disguise.renderYawOffset = source.renderYawOffset;
        disguise.prevRenderYawOffset = source.prevRenderYawOffset;
        disguise.rotationYawHead = source.rotationYawHead;
        disguise.prevRotationYawHead = source.prevRotationYawHead;
        disguise.limbSwing = source.limbSwing;
        disguise.limbSwingAmount = source.limbSwingAmount;
        disguise.prevLimbSwingAmount = source.prevLimbSwingAmount;
        disguise.swingProgress = source.swingProgress;
        disguise.prevSwingProgress = source.prevSwingProgress;
        disguise.ticksExisted = source.ticksExisted;
        disguise.hurtTime = source.hurtTime;
        disguise.deathTime = source.deathTime;
        disguise.onGround = source.onGround;
        disguise.setSneaking(source.isSneaking());
        disguise.setInvisible(source.isInvisible());
        return disguise;
    }

    private EntityLivingBase createDisguise() {
        if (type == HallucinationType.FRIENDLY_ZOMBIES) {
            EntityVillager villager = new EntityVillager(source.world);
            if (source.isChild()) villager.setGrowingAge(-24000);
            return villager;
        }
        EntityZombie zombie = source.getRNG().nextBoolean() ? new EntityZombie(source.world) : new EntityZombieVillager(source.world);
        zombie.setChild(source.isChild());
        return zombie;
    }
}
