package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SnifferEntity extends EntityAnimal {
    public static final int BABY_TICKS = 48000;
    static final ResourceLocation DIGGING_LOOT = Psychedelicraft.id("gameplay/sniffer_digging");
    private static final DataParameter<Integer> ACTIVITY = EntityDataManager.createKey(SnifferEntity.class, DataSerializers.VARINT);
    private static final int EXPLORED_LIMIT = 21;
    private final Deque<BlockPos> explored = new ArrayDeque<>();
    private long nextDig;
    private int exploredDimension;
    private int activityStart;

    public enum Activity {
        IDLE, SCENTING, SNIFFING, SEARCHING, DIGGING, RISING, HAPPY
    }

    public SnifferEntity(World world) {
        super(world);
        setSize(1.9F, 1.75F);
        setPathPriority(PathNodeType.WATER, -1F);
        setPathPriority(PathNodeType.DANGER_FIRE, -1F);
        setPathPriority(PathNodeType.DAMAGE_FIRE, -1F);
        setHealth(getMaxHealth());
    }

    public static void registerLoot() {
        LootTableList.register(DIGGING_LOOT);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ACTIVITY, Activity.IDLE.ordinal());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(.1D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 2));
        tasks.addTask(2, new SnifferBreedGoal(this));
        tasks.addTask(3, new EntityAITempt(this, 1.25, PSItems.TORCHFLOWER_SEEDS, false));
        tasks.addTask(4, new EntityAIFollowParent(this, 1));
        tasks.addTask(5, new SnifferDigGoal(this));
        tasks.addTask(6, new EntityAIWanderAvoidWater(this, 1));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6F));
        tasks.addTask(8, new EntityAILookIdle(this));
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == PSItems.TORCHFLOWER_SEEDS;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        boolean feeding = isBreedingItem(player.getHeldItem(hand));
        boolean accepted = super.processInteract(player, hand);
        if (feeding && accepted && !world.isRemote) playSound(PSSounds.SNIFFER_EAT, 1F, getSoundPitch());
        return accepted;
    }

    @Override
    public SnifferEntity createChild(EntityAgeable parent) {
        SnifferEntity child = new SnifferEntity(world);
        child.setGrowingAge(-BABY_TICKS);
        return child;
    }

    public Activity activity() {
        return Activity.values()[dataManager.get(ACTIVITY)];
    }

    public int activityTicks() {
        return ticksExisted - activityStart;
    }

    void setActivity(Activity next) {
        dataManager.set(ACTIVITY, next.ordinal());
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (ACTIVITY.equals(key)) {
            activityStart = ticksExisted;
            setSize(1.9F, activity() == Activity.DIGGING ? 1.35F : 1.75F);
            setScaleForAge(isChild());
        }
    }

    boolean readyToDig() {
        return world.getTotalWorldTime() >= nextDig && !isChild() && onGround
                && !isInWater() && !isRiding() && !getLeashed() && !isInLove();
    }

    boolean canDig(BlockPos soil) {
        return world.isBlockLoaded(soil) && world.getWorldBorder().contains(soil)
                && (exploredDimension != dimension || !explored.contains(soil))
                && isDiggable(world.getBlockState(soil).getBlock());
    }

    static boolean isDiggable(Block block) {
        return block == Blocks.GRASS || block == Blocks.DIRT || block == PSBlocks.MOSS_BLOCK;
    }

    void recordDig(BlockPos soil) {
        nextDig = world.getTotalWorldTime() + 9600;
        if (exploredDimension != dimension) {
            explored.clear();
            exploredDimension = dimension;
        }
        explored.addFirst(soil);
        if (explored.size() > EXPLORED_LIMIT) explored.removeLast();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote && activity() == Activity.DIGGING && activityTicks() >= 34 && activityTicks() < 120) {
            float angle = rotationYawHead * ((float) Math.PI / 180F);
            double x = posX - MathHelper.sin(angle) * 2.25D;
            double z = posZ + MathHelper.cos(angle) * 2.25D;
            IBlockState ground = world.getBlockState(new BlockPos(x, posY - .2, z));
            int state = Block.getStateId(ground);
            if (ground.getBlock() != Blocks.AIR) {
                for (int i = 0; i < 30; i++) {
                    world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, posY - .25, z,
                            (rand.nextDouble() - .5) * .1, .08, (rand.nextDouble() - .5) * .1, state);
                }
            }
        }
    }

    @Override
    public float getEyeHeight() {
        return height * .6F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return activity() == Activity.IDLE ? PSSounds.SNIFFER_IDLE : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PSSounds.SNIFFER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PSSounds.SNIFFER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(PSSounds.SNIFFER_STEP, .15F, 1F);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setLong("NextDig", nextDig);
        tag.setInteger("ExploredDimension", exploredDimension);
        NBTTagList positions = new NBTTagList();
        for (BlockPos pos : explored) positions.appendTag(new NBTTagLong(pos.toLong()));
        tag.setTag("Explored", positions);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        nextDig = tag.getLong("NextDig");
        exploredDimension = tag.getInteger("ExploredDimension");
        explored.clear();
        NBTTagList positions = tag.getTagList("Explored", Constants.NBT.TAG_LONG);
        for (NBTBase pos : positions) {
            if (explored.size() == EXPLORED_LIMIT) break;
            explored.addLast(BlockPos.fromLong(((NBTTagLong) pos).getLong()));
        }
    }
}
