/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSDamageTypes;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

public final class MolotovCocktailEntity extends EntityThrowable {
    private static final DataParameter<ItemStack> STACK =
            EntityDataManager.createKey(MolotovCocktailEntity.class, DataSerializers.ITEM_STACK);

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

    private float strength() {
        return BrewFluid.projectileStrength(PSItems.MOLOTOV_COCKTAIL.contents(getProjectileStack()));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote && !isDead && strength() > 0) particles(1);
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
        boolean griefing = ForgeEventFactory.getMobGriefingEvent(level, owner == null ? this : owner);
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
        double x = posX + offset(spread);
        double z = posZ + offset(spread);
        y += offset(spread);
        if (world.isRemote) {
            world.spawnParticle(type, x, y, z, 0, 0, 0);
        } else {
            ((WorldServer) world).spawnParticle(type, x, y, z, 1, 0D, 0D, 0D, 0D);
        }
    }

    private double offset(float spread) {
        return (rand.nextDouble() - rand.nextDouble()) * 0.5 * spread;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 0xF000F0;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setTag("Item", getProjectileStack().writeToNBT(new NBTTagCompound()));
        EntityLivingBase owner = getThrower();
        if (owner != null) tag.setString("ownerName", owner.getUniqueID().toString());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
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
