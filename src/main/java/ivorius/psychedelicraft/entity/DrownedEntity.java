package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

public final class DrownedEntity extends EntityZombie {
    private static final ResourceLocation LOOT = Psychedelicraft.id("entities/drowned");
    private static final DataParameter<Boolean> SWIMMING = EntityDataManager.createKey(DrownedEntity.class, DataSerializers.BOOLEAN);
    private final PathNavigate groundNavigation;
    private final PathNavigateSwimmer waterNavigation;
    private boolean seekingSurface;
    private float swimProgress;
    private float previousSwimProgress;

    public DrownedEntity(World world) {
        super(world);
        stepHeight = 1.0F;
        setPathPriority(PathNodeType.WATER, 0);
        groundNavigation = navigator;
        ((PathNavigateGround) groundNavigation).setCanSwim(true);
        waterNavigation = new PathNavigateSwimmer(this, world);
        moveHelper = new SwimMovement();
    }

    public static void registerLoot() {
        LootTableList.register(LOOT);
    }

    public static void registerSpawns() {
        EntitySpawnPlacementRegistry.setPlacementType(DrownedEntity.class, EntityLiving.SpawnPlacementType.IN_WATER);
        for (Biome biome : ForgeRegistries.BIOMES) {
            int weight = spawnWeight(biome);
            if (weight != 0) {
                biome.getSpawnableList(EnumCreatureType.MONSTER).add(new Biome.SpawnListEntry(DrownedEntity.class, weight, 1, 1));
            }
        }
    }

    private static int spawnWeight(Biome biome) {
        if (biome == Biomes.FROZEN_RIVER) return 1;
        if (isRiver(biome)) return biome.isSnowyBiome() ? 1 : 100;
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) ? 5 : 0;
    }

    private static boolean isRiver(Biome biome) {
        return biome == Biomes.RIVER || biome == Biomes.FROZEN_RIVER
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }

    @Override
    public boolean getCanSpawnHere() {
        BlockPos pos = getPosition();
        if (world.getDifficulty() == EnumDifficulty.PEACEFUL
                || world.getBlockState(pos).getMaterial() != Material.WATER
                || world.getBlockState(pos.down()).getMaterial() != Material.WATER
                || world.getLightFor(EnumSkyBlock.BLOCK, pos) > 0) return false;
        boolean river = isRiver(world.getBiome(pos));
        return (river || pos.getY() < world.getSeaLevel() - 5)
                && rand.nextInt(river ? 15 : 40) == 0 && isValidLightLevel();
    }

    @Override
    public boolean isNotColliding() {
        return world.getCollisionBoxes(this, getEntityBoundingBox()).isEmpty()
                && world.checkNoEntityCollision(getEntityBoundingBox(), this);
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SWIMMING, false);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new FindWater());
        tasks.addTask(2, new EntityAIZombieAttack(this, 1, false) {
            @Override
            public boolean shouldExecute() {
                return mayAttack(getAttackTarget()) && super.shouldExecute();
            }

            @Override
            public boolean shouldContinueExecuting() {
                return mayAttack(getAttackTarget()) && super.shouldContinueExecuting();
            }
        });
        tasks.addTask(5, new GoToBeach());
        tasks.addTask(6, new Surface());
        tasks.addTask(7, new EntityAIWander(this, 1));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPigZombie.class));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, 10, true, false, this::mayAttack) {
            @Override
            public boolean shouldContinueExecuting() {
                return mayAttack(getAttackTarget()) && super.shouldContinueExecuting();
            }
        });
        targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    private boolean mayAttack(@Nullable EntityLivingBase target) {
        return target != null && (!world.isDaytime() || target.isInWater());
    }

    @Override
    public void setBreakDoorsAItask(boolean enabled) {
        // Drowned never break doors.
    }

    @Override
    protected boolean shouldBurnInDay() {
        return !isInWater();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByWater() {
        return !dataManager.get(SWIMMING);
    }

    @Override
    public void onLivingUpdate() {
        if (world.isRemote) {
            previousSwimProgress = swimProgress;
            swimProgress = MathHelper.clamp(swimProgress + (dataManager.get(SWIMMING) ? 0.09F : -0.09F), 0, 1);
        } else {
            updateSwimming();
        }
        super.onLivingUpdate();
    }

    private void updateSwimming() {
        EntityLivingBase target = getAttackTarget();
        boolean swimming = !isAIDisabled() && isInWater() && (seekingSurface || target != null && target.isInWater());
        PathNavigate next = swimming ? waterNavigation : groundNavigation;
        if (navigator != next) {
            navigator.clearPath();
            navigator = next;
        }
        dataManager.set(SWIMMING, swimming);
    }

    public float swimProgress(float partialTicks) {
        return previousSwimProgress + (swimProgress - previousSwimProgress) * partialTicks;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (isServerWorld() && dataManager.get(SWIMMING)) {
            moveRelative(strafe, vertical, forward, 0.01F);
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.9;
            motionY *= 0.9;
            motionZ *= 0.9;
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        IEntityLivingData group = super.onInitialSpawn(difficulty, data);
        if (getHeldItemOffhand().isEmpty() && rand.nextFloat() < 0.03F) {
            setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(PSItems.NAUTILUS_SHELL));
            setDropChance(EntityEquipmentSlot.OFFHAND, 2.0F);
        }
        return group;
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (rand.nextFloat() > 0.9F && rand.nextInt(16) >= 10) {
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isInWater() ? PSSounds.DROWNED_AMBIENT_WATER : PSSounds.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damage) {
        return isInWater() ? PSSounds.DROWNED_HURT_WATER : PSSounds.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isInWater() ? PSSounds.DROWNED_DEATH_WATER : PSSounds.DROWNED_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return PSSounds.DROWNED_SWIM;
    }

    @Override
    protected SoundEvent getStepSound() {
        return PSSounds.DROWNED_STEP;
    }

    private final class SwimMovement extends EntityMoveHelper {
        private SwimMovement() {
            super(DrownedEntity.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (!dataManager.get(SWIMMING)) {
                if (!onGround) motionY -= 0.008;
                super.onUpdateMoveHelper();
                return;
            }

            EntityLivingBase target = getAttackTarget();
            if (seekingSurface || target != null && target.posY > entity.posY) motionY += 0.002;
            if (action != Action.MOVE_TO || navigator.noPath()) {
                setAIMoveSpeed(0);
                return;
            }

            double dx = posX - entity.posX;
            double dy = posY - entity.posY;
            double dz = posZ - entity.posZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1e-6) return;
            float yaw = (float) Math.toDegrees(MathHelper.atan2(dz, dx)) - 90;
            rotationYaw = limitAngle(rotationYaw, yaw, 90);
            renderYawOffset = rotationYaw;
            float desiredSpeed = (float) (speed * getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            float swimSpeed = getAIMoveSpeed() + (desiredSpeed - getAIMoveSpeed()) * 0.125F;
            setAIMoveSpeed(swimSpeed);
            motionX += swimSpeed * dx * 0.005;
            motionY += swimSpeed * dy / distance * 0.1;
            motionZ += swimSpeed * dz * 0.005;
        }
    }

    private final class FindWater extends EntityAIBase {
        private BlockPos destination;

        private FindWater() {
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!world.isDaytime() || isInWater()) return false;
            BlockPos origin = getPosition();
            for (int attempt = 0; attempt < 10; attempt++) {
                BlockPos candidate = origin.add(rand.nextInt(20) - 10, 2 - rand.nextInt(8), rand.nextInt(20) - 10);
                if (world.isBlockLoaded(candidate) && Waterlogging.isWater(world, candidate)) {
                    destination = candidate;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !navigator.noPath();
        }

        @Override
        public void startExecuting() {
            navigator.tryMoveToXYZ(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 1);
        }
    }

    private final class GoToBeach extends EntityAIMoveToBlock {
        private GoToBeach() {
            super(DrownedEntity.this, 1, 8);
        }

        @Override
        public boolean shouldExecute() {
            return !world.isDaytime() && isInWater() && posY >= world.getSeaLevel() - 3 && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            seekingSurface = false;
            updateSwimming();
            super.startExecuting();
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.getBlockState(pos).isTopSolid() && world.isAirBlock(pos.up()) && world.isAirBlock(pos.up(2));
        }
    }

    private final class Surface extends EntityAIBase {
        private boolean stuck;

        @Override
        public boolean shouldExecute() {
            return !world.isDaytime() && isInWater() && posY < world.getSeaLevel() - 2;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !stuck && shouldExecute();
        }

        @Override
        public void startExecuting() {
            stuck = false;
            seekingSurface = true;
            updateSwimming();
        }

        @Override
        public void resetTask() {
            seekingSurface = false;
            updateSwimming();
        }

        @Override
        public void updateTask() {
            Path path = navigator.getPath();
            PathPoint end = path == null ? null : path.getFinalPathPoint();
            if (posY >= world.getSeaLevel() - 1 || !navigator.noPath()
                    && (end == null || getDistanceSq(end.x, end.y, end.z) >= 4)) return;

            Vec3d above = new Vec3d(posX, world.getSeaLevel() - 1, posZ);
            Vec3d destination = RandomPositionGenerator.findRandomTargetBlockTowards(DrownedEntity.this, 4, 8, above);
            stuck = destination == null;
            if (!stuck) navigator.tryMoveToXYZ(destination.x, destination.y, destination.z, 1);
        }
    }
}
