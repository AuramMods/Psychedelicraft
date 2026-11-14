package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.BeeNestBlock;
import ivorius.psychedelicraft.block.entity.BeeNestBlockEntity;
import ivorius.psychedelicraft.client.BeeFlightSound;
import ivorius.psychedelicraft.client.particle.NectarParticle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.UUID;

public final class BeeEntity extends EntityAnimal {
    private static final DataParameter<Boolean> NECTAR = EntityDataManager.createKey(BeeEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> ANGRY = EntityDataManager.createKey(BeeEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> STUNG = EntityDataManager.createKey(BeeEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> CROPS_GROWN = EntityDataManager.createKey(BeeEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> ROLLING = EntityDataManager.createKey(BeeEntity.class, DataSerializers.BOOLEAN);
    private static final int POLLINATION_TICKS = 400;
    private static final int WITHOUT_NECTAR_LIMIT = 3600;
    private static final int HOME_SEARCH_RANGE = 20;
    private static final int CROP_GROWTH_LIMIT = 10;

    private BlockPos home;
    private BlockPos flower;
    private int stayOutside;
    private int ticksWithoutNectar;
    private UUID angerTarget;
    private int angerTicks;
    private int ticksSinceSting;
    private float rollAmount;
    private float previousRollAmount;

    public BeeEntity(World world) {
        super(world);
        setSize(0.7F, 0.6F);
        setNoGravity(true);
        setPathPriority(PathNodeType.WATER, -1);
        setPathPriority(PathNodeType.DANGER_FIRE, -1);
        setPathPriority(PathNodeType.DAMAGE_FIRE, -1);
        moveHelper = new Flight();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(NECTAR, false);
        dataManager.register(ANGRY, false);
        dataManager.register(STUNG, false);
        dataManager.register(CROPS_GROWN, 0);
        dataManager.register(ROLLING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(0.6D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2D);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAIAttackMelee(this, 1.4D, true));
        tasks.addTask(2, new EntityAIMate(this, 1D));
        tasks.addTask(3, new FollowFlowers());
        tasks.addTask(5, new EntityAIFollowParent(this, 1.25D));
        tasks.addTask(6, new Forage());
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        PathNavigateFlying navigator = new PathNavigateFlying(this, world);
        navigator.setCanEnterDoors(false);
        return navigator;
    }

    @Override
    public BeeEntity createChild(EntityAgeable parent) {
        return new BeeEntity(world);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        if (!(item.getItem() instanceof ItemBlock)) return false;
        Block block = ((ItemBlock) item.getItem()).getBlock();
        if (block instanceof BlockFlower) return true;
        int meta = item.getMetadata();
        return block == Blocks.DOUBLE_PLANT && meta >= 0 && meta < 6 && BeeNestBlock.isFlower(block.getStateFromMeta(meta));
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    public boolean hasNectar() {
        return dataManager.get(NECTAR);
    }

    public boolean isAngry() {
        return dataManager.get(ANGRY);
    }

    public boolean hasStung() {
        return dataManager.get(STUNG);
    }

    public void angerAt(EntityLivingBase target) {
        if (!canAttack(target) || hasStung()) return;
        angerTarget = target.getUniqueID();
        angerTicks = (20 + rand.nextInt(20)) * 20;
        dataManager.set(ANGRY, true);
        setAttackTarget(target);
    }

    private boolean canAttack(@Nullable EntityLivingBase target) {
        return target != null && target.isEntityAlive() && !isOnSameTeam(target)
                && (!(target instanceof EntityPlayer) || !((EntityPlayer) target).isCreative() && !((EntityPlayer) target).isSpectator());
    }

    private void clearAnger() {
        angerTicks = 0;
        angerTarget = null;
        dataManager.set(ANGRY, false);
        dataManager.set(ROLLING, false);
        setAttackTarget(null);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!super.attackEntityFrom(source, amount)) return false;
        if (!world.isRemote && isEntityAlive() && !hasStung() && source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            if (canAttack(attacker)) {
                angerAt(attacker);
                double range = getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
                for (BeeEntity bee : world.getEntitiesWithinAABB(BeeEntity.class, getEntityBoundingBox().grow(range, 10D, range))) {
                    if (bee != this && bee.getAttackTarget() == null && bee.canEntityBeSeen(attacker)) bee.angerAt(attacker);
                }
            }
        }
        return true;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (world.isRemote || hasStung() || !target.attackEntityFrom(DamageSource.causeMobDamage(this),
                (int) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue())) return false;
        if (target instanceof EntityLivingBase) {
            EntityLivingBase victim = (EntityLivingBase) target;
            EnchantmentHelper.applyThornEnchantments(victim, this);
            EnumDifficulty difficulty = world.getDifficulty();
            int seconds = difficulty == EnumDifficulty.HARD ? 18 : difficulty == EnumDifficulty.NORMAL ? 10 : 0;
            if (seconds > 0) victim.addPotionEffect(new PotionEffect(MobEffects.POISON, seconds * 20));
        }
        EnchantmentHelper.applyArthropodEnchantments(this, target);
        dataManager.set(STUNG, true);
        clearAnger();
        playSound(PSSounds.BEE_STING, 1F, 1F);
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PSSounds.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PSSounds.BEE_DEATH;
    }

    private void setNectar(boolean nectar) {
        dataManager.set(NECTAR, nectar);
        if (nectar) ticksWithoutNectar = 0;
    }

    public void leaveNest(BlockPos pos, boolean delivered, int ticksInside) {
        home = pos.toImmutable();
        stayOutside = 400;
        ticksWithoutNectar = 0;
        if (delivered) {
            setNectar(false);
            dataManager.set(CROPS_GROWN, 0);
        }
        int age = getGrowingAge();
        if (age != 0) setGrowingAge(age < 0 ? Math.min(0, age + ticksInside) : Math.max(0, age - ticksInside));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            previousRollAmount = rollAmount;
            rollAmount = dataManager.get(ROLLING) ? Math.min(1F, rollAmount + 0.2F) : Math.max(0F, rollAmount - 0.24F);
            BeeFlightSound.tick(this);
            if (isEntityAlive() && hasNectar() && dataManager.get(CROPS_GROWN) < CROP_GROWTH_LIMIT) {
                NectarParticle.trySpawn(this);
            }
        }
    }

    public float getRollAmount(float partialTicks) {
        return previousRollAmount + (rollAmount - previousRollAmount) * partialTicks;
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (hasStung() && ++ticksSinceSting % 5 == 0 && rand.nextInt(Math.max(1, 1200 - ticksSinceSting)) == 0) {
            attackEntityFrom(DamageSource.GENERIC, getHealth());
        }
        if (angerTicks > 0) {
            EntityLivingBase target = getAttackTarget();
            if (target == null && angerTarget != null) {
                Entity saved = ((WorldServer) world).getEntityFromUuid(angerTarget);
                if (saved instanceof EntityLivingBase && canAttack((EntityLivingBase) saved)) {
                    target = (EntityLivingBase) saved;
                    setAttackTarget(target);
                }
            }
            if (target != null && !canAttack(target) || (!(target instanceof EntityPlayer) && --angerTicks == 0)) clearAnger();
        }
        EntityLivingBase target = getAttackTarget();
        dataManager.set(ROLLING, isAngry() && !hasStung() && target != null && getDistanceSq(target) < 4D);
        if (stayOutside > 0) stayOutside--;
        if (!hasNectar() && ticksWithoutNectar < WITHOUT_NECTAR_LIMIT) ticksWithoutNectar++;
        pollinateCrops();
    }

    private void pollinateCrops() {
        int grown = dataManager.get(CROPS_GROWN);
        if (!isEntityAlive() || isAngry() || grown >= CROP_GROWTH_LIMIT || !hasNectar() || home == null) return;
        if (((world.getTotalWorldTime() + getEntityId()) & 1L) != 0L || rand.nextFloat() < 0.3F) return;
        BlockPos here = new BlockPos(this);
        if (home.distanceSq(here) >= 32 * 32 || !world.isBlockLoaded(home)
                || !(world.getTileEntity(home) instanceof BeeNestBlockEntity) || rand.nextInt(15) != 0) return;

        // One pass can grow both blocks even if the first reaches the limit.
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos pos = here.down(depth);
            IBlockState state = world.getBlockState(pos);
            IBlockState advanced = nextCropAge(state);
            if (advanced != state && world.setBlockState(pos, advanced, 3)) {
                world.playEvent(2005, pos, 0);
                grown++;
            }
        }
        dataManager.set(CROPS_GROWN, grown);
    }

    static IBlockState nextCropAge(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.BEETROOTS) {
            BlockCrops crop = (BlockCrops) block;
            if (!crop.isMaxAge(state)) return crop.withAge(crop.getMetaFromState(state) + 1);
        } else if (block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM) {
            int age = state.getValue(BlockStem.AGE);
            if (age < 7) return state.withProperty(BlockStem.AGE, age + 1);
        }
        return state;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (home != null) tag.setLong("HivePos", home.toLong());
        if (flower != null) tag.setLong("FlowerPos", flower.toLong());
        tag.setBoolean("HasNectar", hasNectar());
        tag.setInteger("StayOutside", stayOutside);
        tag.setInteger("TicksWithoutNectar", ticksWithoutNectar);
        tag.setInteger("CropsGrownSincePollination", dataManager.get(CROPS_GROWN));
        tag.setBoolean("HasStung", hasStung());
        tag.setInteger("TicksSinceSting", ticksSinceSting);
        tag.setInteger("AngerTime", angerTicks);
        if (angerTarget != null) tag.setUniqueId("AngryAt", angerTarget);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        home = tag.hasKey("HivePos") ? BlockPos.fromLong(tag.getLong("HivePos")) : null;
        flower = tag.hasKey("FlowerPos") ? BlockPos.fromLong(tag.getLong("FlowerPos")) : null;
        setNectar(tag.getBoolean("HasNectar"));
        stayOutside = tag.getInteger("StayOutside");
        ticksWithoutNectar = tag.getInteger("TicksWithoutNectar");
        dataManager.set(CROPS_GROWN, MathHelper.clamp(tag.getInteger("CropsGrownSincePollination"), 0, CROP_GROWTH_LIMIT + 1));
        dataManager.set(STUNG, tag.getBoolean("HasStung"));
        ticksSinceSting = MathHelper.clamp(tag.getInteger("TicksSinceSting"), 0, 1200);
        angerTicks = MathHelper.clamp(tag.getInteger("AngerTime"), 0, 780);
        angerTarget = tag.hasUniqueId("AngryAt") ? tag.getUniqueId("AngryAt") : null;
        dataManager.set(ANGRY, angerTicks > 0 && !hasStung());
    }

    @Nullable
    private BeeNestBlockEntity findHome() {
        if (home != null && world.isBlockLoaded(home) && getDistanceSqToCenter(home) <= 32 * 32) {
            TileEntity tile = world.getTileEntity(home);
            if (tile instanceof BeeNestBlockEntity && !((BeeNestBlockEntity) tile).isFull()) return (BeeNestBlockEntity) tile;
        }
        BlockPos here = new BlockPos(this);
        BeeNestBlockEntity nearest = null;
        double distance = HOME_SEARCH_RANGE * HOME_SEARCH_RANGE;
        WorldServer server = (WorldServer) world;
        for (int x = (here.getX() - HOME_SEARCH_RANGE) >> 4; x <= (here.getX() + HOME_SEARCH_RANGE) >> 4; x++) {
            for (int z = (here.getZ() - HOME_SEARCH_RANGE) >> 4; z <= (here.getZ() + HOME_SEARCH_RANGE) >> 4; z++) {
                Chunk chunk = server.getChunkProvider().getLoadedChunk(x, z);
                if (chunk == null) continue;
                for (TileEntity tile : chunk.getTileEntityMap().values()) {
                    if (!(tile instanceof BeeNestBlockEntity) || tile.isInvalid() || ((BeeNestBlockEntity) tile).isFull()) continue;
                    double candidate = getDistanceSqToCenter(tile.getPos());
                    if (candidate < distance) {
                        nearest = (BeeNestBlockEntity) tile;
                        distance = candidate;
                    }
                }
            }
        }
        if (nearest != null) home = nearest.getPos().toImmutable();
        return nearest;
    }

    private boolean isFlower(BlockPos pos) {
        return world.isBlockLoaded(pos) && BeeNestBlock.isFlower(world.getBlockState(pos));
    }

    @Nullable
    private BlockPos findFlower() {
        if (flower != null && getDistanceSqToCenter(flower) <= 25 && isFlower(flower)) return flower;
        BlockPos here = new BlockPos(this);
        BlockPos nearest = null;
        double distance = 26;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(here.add(-5, -5, -5), here.add(5, 5, 5))) {
            double candidate = getDistanceSqToCenter(pos);
            if (candidate >= distance || !isFlower(pos)) continue;
            nearest = pos.toImmutable();
            distance = candidate;
        }
        return nearest;
    }

    private final class FollowFlowers extends EntityAIBase {
        private EntityPlayer player;

        private FollowFlowers() {
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            player = world.getClosestPlayerToEntity(BeeEntity.this, 10D);
            return player != null && (isBreedingItem(player.getHeldItemMainhand()) || isBreedingItem(player.getHeldItemOffhand()));
        }

        @Override
        public void updateTask() {
            getLookHelper().setLookPositionWithEntity(player, 30F, 30F);
            if (getDistanceSq(player) < 6.25D) getNavigator().clearPath();
            else if (getNavigator().noPath()) getNavigator().tryMoveToEntityLiving(player, 1.25D);
        }

        @Override
        public void resetTask() {
            getNavigator().clearPath();
        }
    }

    private final class Forage extends EntityAIBase {
        private BeeNestBlockEntity nest;
        private BlockPos wanderTarget;
        private int wanderTime;
        private int flowerTime;
        private int lastPollinationSound;
        private int findFlowerDelay;
        private int findHomeDelay;

        private Forage() {
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return true;
        }

        @Override
        public void resetTask() {
            resetPollination();
            wanderTarget = null;
            getNavigator().clearPath();
        }

        @Override
        public void updateTask() {
            boolean resting = !world.isDaytime() || world.isRaining();
            if (!isAngry() && !hasStung() && stayOutside == 0 && (resting || hasNectar() || ticksWithoutNectar == WITHOUT_NECTAR_LIMIT)) {
                resetPollination();
                if (findHomeDelay-- <= 0) {
                    nest = findHome();
                    findHomeDelay = 40;
                }
                if (nest != null && !nest.isInvalid() && !nest.isFull() && world.isBlockLoaded(nest.getPos())) {
                    BlockPos entry = nest.getPos().offset(nest.getFacing());
                    if (world.isBlockLoaded(entry) && world.getBlockState(entry).getCollisionBoundingBox(world, entry) == null) {
                        if (getDistanceSqToCenter(entry) < 0.8D && nest.tryEnter(BeeEntity.this)) return;
                        flyTo(entry, 0.25D);
                        return;
                    }
                }
            }
            if (!resting && !isAngry() && !hasStung() && !hasNectar()) {
                if (flower == null && findFlowerDelay-- <= 0) {
                    flower = findFlower();
                    findFlowerDelay = 20 + rand.nextInt(41);
                }
                if (flower != null) {
                    if (isFlower(flower)) {
                        if (getDistanceSq(flower.getX() + 0.5D, flower.getY() + 0.6D, flower.getZ() + 0.5D) < 0.16D) {
                            getNavigator().clearPath();
                            getMoveHelper().setMoveTo(flower.getX() + 0.5D, flower.getY() + 0.6D, flower.getZ() + 0.5D, 0.35D);
                            ++flowerTime;
                            if (flowerTime > lastPollinationSound + 60 && rand.nextFloat() < 0.05F) {
                                lastPollinationSound = flowerTime;
                                playSound(PSSounds.BEE_POLLINATE, 1F, 1F);
                            }
                            if (flowerTime > POLLINATION_TICKS) {
                                setNectar(true);
                                resetPollination();
                            }
                        } else {
                            flyTo(flower, 0.6D);
                        }
                        return;
                    }
                    flower = null;
                    resetPollination();
                    getNavigator().clearPath();
                }
            }
            if (wanderTarget != null) {
                if (--wanderTime > 0 && getDistanceSqToCenter(wanderTarget) > 1D) {
                    flyTo(wanderTarget, 0.5D);
                    return;
                }
                wanderTarget = null;
                getNavigator().clearPath();
            }
            if (rand.nextInt(20) == 0) {
                BlockPos here = home != null && getDistanceSqToCenter(home) < 64 ? home : new BlockPos(BeeEntity.this);
                BlockPos target = here.add(rand.nextInt(13) - 6, rand.nextInt(5) - 2, rand.nextInt(13) - 6);
                if (world.isBlockLoaded(target) && world.isAirBlock(target)) {
                    wanderTarget = target;
                    wanderTime = 100;
                }
            }
        }

        private void resetPollination() {
            flowerTime = 0;
            lastPollinationSound = 0;
        }

        private void flyTo(BlockPos target, double height) {
            if (!world.isBlockLoaded(target)) return;
            Vec3d destination = new Vec3d(target.getX() + 0.5D, target.getY() + height, target.getZ() + 0.5D);
            if (world.rayTraceBlocks(new Vec3d(posX, posY + BeeEntity.this.height * 0.5D, posZ),
                    destination.addVector(0, BeeEntity.this.height * 0.5D, 0), false, true, false) == null) {
                getNavigator().clearPath();
                getMoveHelper().setMoveTo(destination.x, destination.y, destination.z, 1D);
            } else if (getNavigator().noPath() && ticksExisted % 10 == 0) {
                getNavigator().tryMoveToXYZ(destination.x, destination.y, destination.z, 1D);
            }
        }
    }

    private final class Flight extends EntityMoveHelper {
        private Flight() {
            super(BeeEntity.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (action != Action.MOVE_TO) {
                motionX *= 0.6D;
                motionY *= 0.6D;
                motionZ *= 0.6D;
                return;
            }
            action = Action.WAIT;
            Vec3d delta = new Vec3d(this.posX - BeeEntity.this.posX, this.posY - BeeEntity.this.posY, this.posZ - BeeEntity.this.posZ);
            double step = Math.min(delta.lengthVector(), speed * getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getAttributeValue() * 0.27D);
            Vec3d velocity = delta.normalize().scale(step);
            motionX = motionX * 0.3D + velocity.x * 0.7D;
            motionY = motionY * 0.3D + velocity.y * 0.7D;
            motionZ = motionZ * 0.3D + velocity.z * 0.7D;
            if (motionX * motionX + motionZ * motionZ > 1.0E-5D) {
                rotationYaw = limitAngle(rotationYaw, (float) Math.toDegrees(MathHelper.atan2(motionZ, motionX)) - 90F, 10F);
            }
        }
    }
}
