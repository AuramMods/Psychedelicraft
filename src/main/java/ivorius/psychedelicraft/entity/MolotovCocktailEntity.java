/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.block.CampfireBlock;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.item.MolotovCocktailItem;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

public final class MolotovCocktailEntity extends EntityThrowable implements IEntityAdditionalSpawnData {
    private static final double SPAWN_VELOCITY_SCALE = 8000;
    private static final double MAX_SPAWN_VELOCITY = 3.9;
    private static final DataParameter<ItemStack> STACK =
            EntityDataManager.createKey(MolotovCocktailEntity.class, DataSerializers.ITEM_STACK);
    private boolean leftOwner;

    public MolotovCocktailEntity(World world) {
        super(world);
        setSize(0.1F, 0.1F);
    }

    public MolotovCocktailEntity(World world, EntityLivingBase owner, ItemStack stack) {
        super(world, owner);
        setSize(0.1F, 0.1F);
        dataManager.set(STACK, flyingStack(stack));
    }

    @Override
    protected void entityInit() {
        dataManager.register(STACK, flyingStack(ItemStack.EMPTY));
    }

    private static ItemStack flyingStack(ItemStack stack) {
        ItemStack single = stack.getItem() == PSItems.MOLOTOV_COCKTAIL ? stack.copy() : new ItemStack(PSItems.MOLOTOV_COCKTAIL);
        single.setCount(1);
        if (!single.hasTagCompound()) single.setTagCompound(new NBTTagCompound());
        single.getTagCompound().setBoolean(MolotovCocktailItem.FLYING, true);
        return single;
    }

    public ItemStack getProjectileStack() {
        return dataManager.get(STACK);
    }

    @Override
    public void writeSpawnData(ByteBuf data) {
        EntityLivingBase owner = getThrower();
        data.writeInt(owner == null ? -1 : owner.getEntityId());
        writeSpawnVelocity(data, motionX);
        writeSpawnVelocity(data, motionY);
        writeSpawnVelocity(data, motionZ);
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        if (!data.isReadable()) return;
        int ownerId = data.readInt();
        Entity owner = ownerId == -1 ? null : world.getEntityByID(ownerId);
        thrower = owner instanceof EntityLivingBase ? (EntityLivingBase) owner : null;
        setVelocity(data.readShort() / SPAWN_VELOCITY_SCALE,
                data.readShort() / SPAWN_VELOCITY_SCALE, data.readShort() / SPAWN_VELOCITY_SCALE);
    }

    private static void writeSpawnVelocity(ByteBuf data, double motion) {
        data.writeShort((int) (MathHelper.clamp(motion, -MAX_SPAWN_VELOCITY, MAX_SPAWN_VELOCITY)
                * SPAWN_VELOCITY_SCALE));
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float inaccuracy) {
        double length = Math.sqrt(x * x + y * y + z * z);
        Vec3d direction = length < 1E-4 ? Vec3d.ZERO : new Vec3d(x / length, y / length, z / length);
        double spread = 0.0172275 * inaccuracy;
        motionX = (direction.x + triangularOffset(spread)) * speed;
        motionY = (direction.y + triangularOffset(spread)) * speed;
        motionZ = (direction.z + triangularOffset(spread)) * speed;
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) Math.toDegrees(MathHelper.atan2(motionX, motionZ));
        rotationPitch = (float) Math.toDegrees(MathHelper.atan2(motionY, horizontal));
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
    }

    private float strength() {
        return BrewFluid.projectileStrength(PSItems.MOLOTOV_COCKTAIL.contents(getProjectileStack()));
    }

    @Override
    public void onUpdate() {
        if (!leftOwner) {
            Entity owner = getThrower();
            leftOwner = owner == null || world.getEntitiesInAABBexcluding(this, flightBounds(),
                    entity -> EntitySelectors.NOT_SPECTATING.apply(entity) && entity.canBeCollidedWith()
                            && entity.isRidingSameEntity(owner)).isEmpty();
        }
        lastTickPosX = posX;
        lastTickPosY = posY;
        lastTickPosZ = posZ;
        if (!world.isRemote) setFlag(6, isGlowing());
        onEntityUpdate();
        if (isDead) return;

        Vec3d start = getPositionVector();
        Vec3d end = start.addVector(motionX, motionY, motionZ);
        RayTraceResult hit = ProjectileBlockCollision.trace(this, start, end);
        hit = closestHit(start, hit == null ? end : hit.hitVec, hit);
        if (hit != null && !CampfireBlock.afterProjectileImpact(this, hit, ForgeEventFactory.onProjectileImpact(this, hit))) {
            onImpact(hit);
        }

        doBlockCollisions();
        double x = posX + motionX;
        double y = posY + motionY;
        double z = posZ + motionZ;
        float yaw = (float) Math.toDegrees(MathHelper.atan2(motionX, motionZ));
        float pitch = (float) Math.toDegrees(MathHelper.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
        rotationYaw = prevRotationYaw + MathHelper.wrapDegrees(yaw - prevRotationYaw) * 0.2F;
        rotationPitch = prevRotationPitch + MathHelper.wrapDegrees(pitch - prevRotationPitch) * 0.2F;

        float drag = 0.99F;
        if (isInWater()) {
            for (int i = 0; i < 4; i++) {
                world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, x - motionX * 0.25D, y - motionY * 0.25D,
                        z - motionZ * 0.25D, motionX, motionY, motionZ);
            }
            drag = 0.8F;
        }
        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;
        if (!hasNoGravity()) motionY -= getGravityVelocity();
        setPosition(x, y, z);
        if (world.isRemote && !isDead && strength() > 0) particles(1);
    }

    private AxisAlignedBB flightBounds() {
        return getEntityBoundingBox().expand(motionX, motionY, motionZ).grow(1);
    }

    private RayTraceResult closestHit(Vec3d start, Vec3d end, RayTraceResult closest) {
        Entity owner = getThrower();
        double distance = Double.MAX_VALUE;
        for (Entity entity : world.getEntitiesInAABBexcluding(this, flightBounds(), candidate ->
                EntitySelectors.NOT_SPECTATING.apply(candidate) && candidate.isEntityAlive() && candidate.canBeCollidedWith()
                        && (leftOwner || owner == null || !candidate.isRidingSameEntity(owner)))) {
            RayTraceResult hit = ProjectileBlockCollision.entry(entity.getEntityBoundingBox().grow(0.3F), start, end);
            if (hit == null) continue;
            double hitDistance = start.squareDistanceTo(hit.hitVec);
            if (hitDistance < distance) {
                distance = hitDistance;
                closest = new RayTraceResult(entity);
            }
        }
        return closest;
    }

    @Override
    protected void onImpact(RayTraceResult hit) {
        if (world.isRemote || isDead) return;
        float strength = strength();
        float blast = strength * 0.6F;
        float fire = strength * 2;
        if (hit.typeOfHit == RayTraceResult.Type.ENTITY) {
            damage(hit.entityHit, blast, fire, 1);
            if (blast > 0) {
                for (Entity nearby : world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().grow(blast))) {
                    if (nearby == hit.entityHit) continue;
                    float distance = getDistance(nearby);
                    if (distance < blast) damage(nearby, blast, fire, 1 - distance / blast);
                }
            }
        }

        playSound(SoundEvents.BLOCK_GLASS_BREAK, 1, 1);
        for (int count = MathHelper.ceil(fire * 2); count > 0; count--) particles(fire);
        if (blast > 0) {
            explode((WorldServer) world, blast, fire > 0);
        } else {
            playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1, 1);
        }
        setDead();
    }

    private void damage(Entity target, float blast, float fire, float scale) {
        target.attackEntityFrom(PSDamageTypes.molotov(this, getThrower(), target, getProjectileStack()),
                scale * Math.max(4, blast * 0.6F + fire * 0.3F));
        if (fire > 0) FireTicks.set(target, (int) Math.max(10, fire * 3));
    }

    private void explode(WorldServer level, float blast, boolean fire) {
        EntityLivingBase owner = getThrower();
        boolean griefing = ForgeEventFactory.getMobGriefingEvent(level, this);
        Explosion explosion = new Explosion(level, this, posX, posY, posZ, blast, fire, griefing) {
            @Override
            public EntityLivingBase getExplosivePlacedBy() {
                return owner;
            }
        };
        if (ForgeEventFactory.onExplosionStart(level, explosion)) return;
        explosion.doExplosionA();
        explosion.doExplosionB(false);
        if (!griefing) explosion.clearAffectedBlockPositions();
        for (EntityPlayer player : level.playerEntities) {
            if (player instanceof EntityPlayerMP && player.getDistanceSq(posX, posY, posZ) < 4096) {
                ((EntityPlayerMP) player).connection.sendPacket(new SPacketExplosion(posX, posY, posZ, blast,
                        explosion.getAffectedBlockPositions(), explosion.getPlayerKnockbackMap().get(player)));
            }
        }
    }

    @Override
    public boolean canExplosionDestroyBlock(Explosion explosion, World level, BlockPos pos, IBlockState state, float power) {
        return state.getBlock().isReplaceable(level, pos);
    }

    private void particles(float spread) {
        particle(EnumParticleTypes.FLAME, posY, spread);
        particle(EnumParticleTypes.LAVA, posY + height, spread);
    }

    private void particle(EnumParticleTypes type, double y, float spread) {
        double x = posX + triangularOffset(0.5 * spread);
        double z = posZ + triangularOffset(0.5 * spread);
        y += triangularOffset(0.5 * spread);
        if (world.isRemote) {
            world.spawnParticle(type, x, y, z, 0, 0, 0);
        } else {
            ((WorldServer) world).spawnParticle(type, x, y, z, 1, 0D, 0D, 0D, 0D);
        }
    }

    private double triangularOffset(double deviation) {
        return deviation * (rand.nextDouble() - rand.nextDouble());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (leftOwner) tag.setBoolean("LeftOwner", true);
        tag.setTag("Item", getProjectileStack().writeToNBT(new NBTTagCompound()));
        EntityLivingBase owner = getThrower();
        if (owner != null) tag.setString("ownerName", owner.getUniqueID().toString());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        leftOwner = tag.getBoolean("LeftOwner");
        dataManager.set(STACK, flyingStack(new ItemStack(tag.getCompoundTag("Item"))));
    }

    private static final class FireTicks {
        private static final Field FIELD = ReflectionHelper.findField(Entity.class, "fire", "field_190534_ay");

        private static void set(Entity entity, int ticks) {
            try {
                FIELD.setInt(entity, ticks);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Could not set entity fire ticks", failure);
            }
        }
    }
}
