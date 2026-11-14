package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

final class SnifferBreedGoal extends EntityAIBase {
    private final SnifferEntity animal;
    private SnifferEntity mate;
    private int ticks;

    SnifferBreedGoal(SnifferEntity animal) {
        this.animal = animal;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!animal.isInLove()) return false;
        double nearest = Double.POSITIVE_INFINITY;
        for (SnifferEntity other : animal.world.getEntitiesWithinAABB(SnifferEntity.class, animal.getEntityBoundingBox().grow(8))) {
            double distance = animal.getDistanceSq(other);
            if (distance < nearest && animal.canMateWith(other)) {
                nearest = distance;
                mate = other;
            }
        }
        return mate != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return mate != null && mate.isEntityAlive() && animal.canMateWith(mate) && ticks < 60;
    }

    @Override
    public void resetTask() {
        mate = null;
        ticks = 0;
    }

    @Override
    public void updateTask() {
        animal.getLookHelper().setLookPositionWithEntity(mate, 10F, animal.getVerticalFaceSpeed());
        animal.getNavigator().tryMoveToEntityLiving(mate, 1);
        if (++ticks >= 60 && animal.getDistanceSq(mate) < 9) layEgg();
    }

    private void layEgg() {
        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(animal, mate, null);
        boolean canceled = MinecraftForge.EVENT_BUS.post(event);
        animal.setGrowingAge(6000);
        mate.setGrowingAge(6000);
        animal.resetInLove();
        mate.resetInLove();
        if (canceled) return;

        EntityAgeable replacement = event.getChild();
        if (replacement == null) {
            animal.entityDropItem(new ItemStack(PSItems.SNIFFER_EGG), 0F);
            animal.playSound(PSSounds.SNIFFER_EGG_PLOP, 1F, .5F + (animal.getRNG().nextFloat() - animal.getRNG().nextFloat()) * .2F);
        } else {
            replacement.setGrowingAge(-SnifferEntity.BABY_TICKS);
            replacement.setLocationAndAngles(animal.posX, animal.posY, animal.posZ, animal.rotationYaw, 0F);
            animal.world.spawnEntity(replacement);
        }
        if (event.getCausedByPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getCausedByPlayer();
            player.addStat(StatList.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(player, animal, mate, replacement);
        }
        animal.world.setEntityState(animal, (byte) 18);
        if (animal.world.getGameRules().getBoolean("doMobLoot")) {
            animal.world.spawnEntity(new EntityXPOrb(animal.world, animal.posX, animal.posY, animal.posZ, animal.getRNG().nextInt(7) + 1));
        }
    }
}
