package ivorius.psychedelicraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class TraderLlamaEntity extends EntityLlama {
    private int despawnDelay = 47999;

    public TraderLlamaEntity(World world) {
        super(world);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        tasks.addTask(1, new EntityAIPanic(this, 2D));
        targetTasks.addTask(1, new DefendTrader());
    }

    @Override
    protected void mountTo(EntityPlayer player) {
        if (!(getLeashHolder() instanceof WanderingTraderEntity)) super.mountTo(player);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || isTame() || getPassengers().size() == 1 && getPassengers().get(0) instanceof EntityPlayer) return;

        Entity holder = getLeashHolder();
        if (holder instanceof WanderingTraderEntity) {
            despawnDelay = ((WanderingTraderEntity) holder).getDespawnDelay();
        } else if (getLeashed()) {
            return;
        }
        if (--despawnDelay <= 0) {
            clearLeashed(true, false);
            setDead();
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("DespawnDelay", despawnDelay);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("DespawnDelay", 99)) despawnDelay = tag.getInteger("DespawnDelay");
    }

    private final class DefendTrader extends EntityAITarget {
        private EntityLivingBase attacker;
        private int lastAttack;

        private DefendTrader() {
            super(TraderLlamaEntity.this, false);
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            Entity holder = getLeashHolder();
            if (!(holder instanceof WanderingTraderEntity)) return false;
            WanderingTraderEntity trader = (WanderingTraderEntity) holder;
            attacker = trader.getRevengeTarget();
            return trader.getRevengeTimer() != lastAttack && isSuitableTarget(attacker, false);
        }

        @Override
        public void startExecuting() {
            setAttackTarget(attacker);
            Entity holder = getLeashHolder();
            if (holder instanceof WanderingTraderEntity) lastAttack = ((WanderingTraderEntity) holder).getRevengeTimer();
            super.startExecuting();
        }
    }
}
