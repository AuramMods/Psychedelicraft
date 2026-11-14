package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.inventory.EntityEquipmentSlot;

import javax.annotation.Nullable;

final class IdentitySwapHallucination {
    private final HallucinationType type;
    private int age;
    private EntityLiving source;
    private EntityLiving disguise;

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
            source = (EntityLiving) original;
            disguise = createDisguise();
            disguise.setUniqueId(source.getUniqueID());
            disguise.setCustomNameTag(source.getCustomNameTag());
            disguise.setAlwaysRenderNameTag(source.getAlwaysRenderNameTag());
            disguise.setLeftHanded(source.isLeftHanded());
            disguise.setSilent(true);
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                disguise.setItemStackToSlot(slot, source.getItemStackFromSlot(slot).copy());
            }
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
        disguise.swingingHand = source.swingingHand;
        disguise.ticksExisted = source.ticksExisted;
        disguise.hurtTime = source.hurtTime;
        disguise.deathTime = source.deathTime;
        disguise.onGround = source.onGround;
        disguise.setSneaking(source.isSneaking());
        disguise.setInvisible(source.isInvisible());
        disguise.setGlowing(source.isGlowing());
        if (source.isBurning()) {
            if (!disguise.isBurning()) disguise.setFire(1);
        } else {
            disguise.extinguish();
        }
        return disguise;
    }

    private EntityLiving createDisguise() {
        if (type == HallucinationType.FRIENDLY_ZOMBIES) {
            EntityVillager villager = new EntityVillager(source.world);
            if (source.isChild()) villager.setGrowingAge(-24000);
            return villager;
        }
        EntityZombie zombie;
        if (source.getRNG().nextBoolean()) {
            zombie = new EntityZombie(source.world);
        } else {
            EntityZombieVillager villager = new EntityZombieVillager(source.world);
            villager.setForgeProfession(((EntityVillager) source).getProfessionForge());
            zombie = villager;
        }
        zombie.setChild(source.isChild());
        return zombie;
    }
}
