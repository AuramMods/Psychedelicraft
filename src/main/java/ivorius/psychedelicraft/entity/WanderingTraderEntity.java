package ivorius.psychedelicraft.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.INpc;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

public final class WanderingTraderEntity extends EntityCreature implements INpc, IMerchant {
    private MerchantRecipeList offers;
    private EntityPlayer customer;
    private int despawnDelay;

    public WanderingTraderEntity(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new TradeWithCustomer());
        tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityZombie.class, 8F, 0.5D, 0.5D));
        tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityEvoker.class, 12F, 0.5D, 0.5D));
        tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityVindicator.class, 8F, 0.5D, 0.5D));
        tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityVex.class, 8F, 0.5D, 0.5D));
        tasks.addTask(1, new EntityAIPanic(this, 0.5D));
        tasks.addTask(4, new EntityAIMoveTowardsRestriction(this, 0.35D));
        tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.35D));
        tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 8F));
        tasks.addTask(10, new EntityAILookIdle(this));
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Items.NAME_TAG) return held.interactWithEntity(player, this, hand);
        if (!isEntityAlive() || customer != null) return super.processInteract(player, hand);
        if (!world.isRemote) {
            if (hand == EnumHand.MAIN_HAND) player.addStat(StatList.TALKED_TO_VILLAGER);
            MerchantRecipeList available = getRecipes(player);
            if (available != null && !available.isEmpty()) {
                setCustomer(player);
                player.displayVillagerTradeGui(this);
            }
        }
        return true;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) return;
        if (customer != null && (!isEntityAlive() || !customer.isEntityAlive() || customer.world != world
                || getDistanceSq(customer) > 16D || isInWater())) setCustomer(null);
        if (customer == null && despawnDelay > 0 && --despawnDelay == 0) setDead();
    }

    public void setDespawnDelay(int ticks) {
        despawnDelay = ticks;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public void setCustomer(@Nullable EntityPlayer player) {
        customer = player;
    }

    @Nullable
    @Override
    public EntityPlayer getCustomer() {
        return isEntityAlive() ? customer : null;
    }

    @Override
    public MerchantRecipeList getRecipes(EntityPlayer player) {
        if (offers == null) offers = WanderingTraderTrades.draw(rand);
        return ForgeEventFactory.listTradeOffers(this, player, offers);
    }

    @Override
    public void setRecipes(@Nullable MerchantRecipeList recipes) {
        offers = recipes;
    }

    @Override
    public void useRecipe(MerchantRecipe recipe) {
        recipe.incrementToolUses();
        livingSoundTime = -getTalkInterval();
        playSound(SoundEvents.ENTITY_VILLAGER_YES, getSoundVolume(), getSoundPitch());
        if (!world.isRemote && recipe.getRewardsExp()) {
            world.spawnEntity(new EntityXPOrb(world, posX, posY + 0.5D, posZ, 3 + rand.nextInt(4)));
        }
    }

    @Override
    public void verifySellingItem(ItemStack stack) {
        if (!world.isRemote && livingSoundTime > -getTalkInterval() + 20) {
            livingSoundTime = -getTalkInterval();
            playSound(stack.isEmpty() ? SoundEvents.ENTITY_VILLAGER_NO : SoundEvents.ENTITY_VILLAGER_YES,
                    getSoundVolume(), getSoundPitch());
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return getPosition();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return customer == null ? SoundEvents.ENTITY_VILLAGER_AMBIENT : SoundEvents.ENTITY_VILLAGER_TRADING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (offers != null) tag.setTag("Offers", offers.getRecipiesAsTags());
        tag.setInteger("DespawnDelay", despawnDelay);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        offers = tag.hasKey("Offers", 10) ? new MerchantRecipeList(tag.getCompoundTag("Offers")) : null;
        despawnDelay = tag.getInteger("DespawnDelay");
    }

    private final class TradeWithCustomer extends EntityAIBase {
        private TradeWithCustomer() {
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            return customer != null;
        }

        @Override
        public void startExecuting() {
            getNavigator().clearPath();
        }

        @Override
        public void updateTask() {
            getLookHelper().setLookPositionWithEntity(customer, 10F, getVerticalFaceSpeed());
        }
    }
}
