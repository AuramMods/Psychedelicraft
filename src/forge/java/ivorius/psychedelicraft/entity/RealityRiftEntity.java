/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public final class RealityRiftEntity extends Entity {
    private static final DataParameter<Float> SIZE = EntityDataManager.createKey(RealityRiftEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> INSTABILITY = EntityDataManager.createKey(RealityRiftEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> CLOSING = EntityDataManager.createKey(RealityRiftEntity.class, DataSerializers.BOOLEAN);
    private float visualSize;
    private float previousVisualSize;

    public RealityRiftEntity(World world) {
        super(world);
        setSize(2, 2);
        setNoGravity(true);
        if (!world.isRemote) setRiftSize(0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.5F);
    }

    public static void spawnNear(Entity entity) {
        World world = entity.world;
        RealityRiftEntity rift = new RealityRiftEntity(world);
        rift.setPosition(entity.posX + (world.rand.nextDouble() - 0.5) * 100,
                entity.posY + (world.rand.nextDouble() - 0.5) * 100,
                entity.posZ + (world.rand.nextDouble() - 0.5) * 100);
        world.spawnEntity(rift);
    }

    @Override
    protected void entityInit() {
        dataManager.register(SIZE, 0F);
        dataManager.register(INSTABILITY, 0F);
        dataManager.register(CLOSING, false);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(20);
    }

    @Override
    public ITextComponent getDisplayName() {
        ITextComponent name = super.getDisplayName();
        name.getStyle().setObfuscated(true);
        return name;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    public float getRiftSize() {
        return dataManager.get(SIZE);
    }

    public void setRiftSize(float size) {
        dataManager.set(SIZE, Float.isFinite(size) ? Math.max(0, size) : 0);
    }

    public float takeFromRift(float amount) {
        if (isCritical()) return 0.2F;
        float before = getRiftSize();
        setRiftSize(before - amount);
        return before - getRiftSize();
    }

    public float getInstability() {
        return dataManager.get(INSTABILITY);
    }

    public boolean isRiftClosing() {
        return dataManager.get(CLOSING);
    }

    public boolean isCritical() {
        return !isRiftClosing() && (getInstability() > 0 || getRiftSize() > 3);
    }

    public float getVisualSize(float partialTicks) {
        return previousVisualSize + (visualSize - previousVisualSize) * partialTicks;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        motionX = motionY = motionZ = 0;
        if (world.isRemote) {
            spawnParticles();
        } else {
            tickRift();
        }
        previousVisualSize = visualSize;
        float target = getRiftSize();
        float interpolated = visualSize + (target - visualSize) * 0.05F;
        visualSize = interpolated < target ? Math.min(target, interpolated + 0.005F)
                : Math.max(target, interpolated - 0.005F);
        if (!world.isRemote && visualSize <= 0 && target <= 0) setDead();
    }

    private void tickRift() {
        boolean critical = isCritical();
        float range = 5 + getInstability() * 50;
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class,
                getEntityBoundingBox().grow(range), EntitySelectors.CAN_AI_TARGET)) {
            double dose = (range - getDistance(target)) * 0.0005 * getRiftSize();
            if (dose <= 0) continue;
            DrugProperties drugs = target instanceof EntityPlayer ? DrugCapability.get((EntityPlayer) target) : null;
            if (drugs != null) {
                drugs.addToDrug(DrugType.ZERO, dose * 20);
                drugs.addToDrug(DrugType.POWER, dose * 200);
            } else if (critical) {
                target.attackEntityFrom(DamageSource.MAGIC, (float) (dose * 20));
            }
        }

        if (critical) {
            float previous = getInstability();
            float instability = Math.min(previous + 0.001F, 1);
            dataManager.set(INSTABILITY, instability);
            replaceShell(previous * 50, instability * 50);
            if (instability >= 0.9F) dataManager.set(CLOSING, true);
        } else {
            setRiftSize(getRiftSize() - (isRiftClosing() ? 0.05F : 1F / 24000F));
        }
    }

    private void replaceShell(float previousRadius, float radius) {
        BlockPos center = getPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double outerSquared = radius * radius;
        double innerSquared = previousRadius * previousRadius;
        int extent = MathHelper.ceil(radius) - 1;
        for (int x = -extent; x <= extent; x++) {
            for (int y = -extent; y <= extent; y++) {
                int rowSquared = x * x + y * y;
                if (rowSquared >= outerSquared || center.getY() + y < 0 || center.getY() + y >= world.getHeight()) {
                    continue;
                }
                int end = MathHelper.ceil(Math.sqrt(outerSquared - rowSquared)) - 1;
                if (rowSquared >= innerSquared) {
                    replaceRow(center, cursor, x, y, -end, end);
                } else {
                    int start = MathHelper.ceil(Math.sqrt(innerSquared - rowSquared));
                    replaceRow(center, cursor, x, y, -end, -start);
                    replaceRow(center, cursor, x, y, start, end);
                }
            }
        }
    }

    private void replaceRow(BlockPos center, BlockPos.MutableBlockPos cursor, int x, int y, int from, int to) {
        for (int z = from; z <= to; z++) {
            cursor.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
            if (world.isBlockLoaded(cursor) && !world.isAirBlock(cursor)) {
                world.setBlockState(cursor, PSBlocks.GLITCH.getDefaultState());
            }
        }
    }

    private void spawnParticles() {
        for (int count = rand.nextInt(3); count > 0; count--) {
            float spread = rand.nextFloat() * rand.nextFloat();
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + particleOffset(spread),
                    posY + height * 0.5 + particleOffset(spread), posZ + particleOffset(spread), 0, 0, 0);
        }
        if (rand.nextBoolean()) {
            float spread = rand.nextFloat() * rand.nextFloat();
            world.spawnParticle(EnumParticleTypes.REDSTONE, posX + particleOffset(spread),
                    posY + height * 0.5 + particleOffset(spread), posZ + particleOffset(spread), 1, 0.5, 0.5);
        }
        float spread = rand.nextFloat() * rand.nextFloat();
        world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, posX, posY + 1 + height * 0.5, posZ,
                particleOffset(spread), particleOffset(spread) + height * 0.5, particleOffset(spread));
    }

    private float particleOffset(float spread) {
        return (rand.nextFloat() * 8 - 4) * spread;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setRiftSize(tag.getFloat("riftSize"));
        float instability = tag.getFloat("instability");
        dataManager.set(INSTABILITY, Float.isFinite(instability) ? MathHelper.clamp(instability, 0, 1) : 0);
        dataManager.set(CLOSING, tag.getBoolean("isRiftClosing"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setFloat("riftSize", getRiftSize());
        tag.setFloat("instability", getInstability());
        tag.setBoolean("isRiftClosing", isRiftClosing());
    }
}
