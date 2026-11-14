package ivorius.psychedelicraft.entity;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.TridentEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ThrownTridentEntity extends EntityArrow implements IEntityAdditionalSpawnData {
    private static final DataParameter<Boolean> FOIL = EntityDataManager.createKey(ThrownTridentEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Byte> LOYALTY = EntityDataManager.createKey(ThrownTridentEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> RETURNING = EntityDataManager.createKey(ThrownTridentEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DEALT_DAMAGE = EntityDataManager.createKey(ThrownTridentEntity.class, DataSerializers.BOOLEAN);
    private ItemStack trident;
    private int ownerEntityId = -1;
    private UUID ownerId;

    public ThrownTridentEntity(World world) {
        super(world);
        trident = new ItemStack(PSItems.TRIDENT);
    }

    public ThrownTridentEntity(World world, EntityLivingBase owner, ItemStack stack) {
        super(world, owner);
        ownerId = owner.getUniqueID();
        trident = stack.copy();
        syncItem();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(FOIL, false);
        dataManager.register(LOYALTY, (byte) 0);
        dataManager.register(RETURNING, false);
        dataManager.register(DEALT_DAMAGE, false);
    }

    public boolean hasFoil() {
        return dataManager.get(FOIL);
    }

    private void syncItem() {
        dataManager.set(FOIL, trident.isItemEnchanted());
        dataManager.set(LOYALTY, (byte) EnchantmentHelper.getEnchantmentLevel(TridentEnchantments.LOYALTY, trident));
    }

    @Override
    public void onUpdate() {
        if (shootingEntity != null && shootingEntity.world != world) shootingEntity = null;
        if (shootingEntity == null) {
            if (ownerId != null && world instanceof WorldServer) {
                shootingEntity = ((WorldServer) world).getEntityFromUuid(ownerId);
            } else if (world.isRemote && ownerEntityId >= 0) {
                shootingEntity = world.getEntityByID(ownerEntityId);
            }
        }
        if (!world.isRemote && timeInGround > 4) dataManager.set(DEALT_DAMAGE, true);
        int loyalty = dataManager.get(LOYALTY);
        if (loyalty > 0 && dataManager.get(DEALT_DAMAGE) && shootingEntity != null) {
            if (!world.isRemote && (!shootingEntity.isEntityAlive() || shootingEntity instanceof EntityPlayer
                    && ((EntityPlayer) shootingEntity).isSpectator())) {
                if (pickupStatus == PickupStatus.ALLOWED) entityDropItem(getArrowStack(), 0.1F);
                setDead();
                return;
            }
            if (!world.isRemote && !dataManager.get(RETURNING)) {
                dataManager.set(RETURNING, true);
                playSound(PSSounds.TRIDENT_RETURN, 10, 1);
            }
        }
        if (dataManager.get(RETURNING)) {
            if (shootingEntity != null) returnToOwner(loyalty);
        } else {
            super.onUpdate();
        }
    }

    private void returnToOwner(int loyalty) {
        noClip = true;
        inGround = false;
        arrowShake = 0;
        lastTickPosX = posX;
        lastTickPosY = posY;
        lastTickPosZ = posZ;
        onEntityUpdate();

        Vec3d eye = shootingEntity.getPositionEyes(1);
        Vec3d start = getPositionVector();
        Vec3d offset = eye.subtract(start);
        Vec3d acceleration = offset.normalize().scale(0.05 * loyalty);
        motionX = motionX * 0.95 + acceleration.x;
        motionY = motionY * 0.95 + acceleration.y;
        motionZ = motionZ * 0.95 + acceleration.z;
        Vec3d end = start.addVector(motionX, motionY + offset.y * 0.015 * loyalty, motionZ);
        setPosition(end.x, end.y, end.z);
        float yaw = (float) Math.toDegrees(MathHelper.atan2(motionX, motionZ));
        float pitch = (float) Math.toDegrees(MathHelper.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * 0.2F;
        rotationPitch += MathHelper.wrapDegrees(pitch - rotationPitch) * 0.2F;
        if (!world.isRemote && shootingEntity instanceof EntityPlayer) {
            AxisAlignedBB pickupArea = shootingEntity.getEntityBoundingBox().grow(0.35);
            if (pickupArea.intersects(getEntityBoundingBox()) || pickupArea.calculateIntercept(start, end) != null) {
                onCollideWithPlayer((EntityPlayer) shootingEntity);
            }
        }
    }

    @Override
    @Nullable
    protected Entity findEntityOnPath(Vec3d start, Vec3d end) {
        return dataManager.get(DEALT_DAMAGE) ? null : super.findEntityOnPath(start, end);
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        Entity target = hit.entityHit;
        if (target == null) {
            super.onHit(hit);
            return;
        }
        if (world.isRemote) return;
        dataManager.set(DEALT_DAMAGE, true);
        Entity attacker = shootingEntity == null ? this : shootingEntity;
        EntityDamageSourceIndirect source = new EntityDamageSourceIndirect("trident", this, attacker);
        source.setProjectile();
        if (target.attackEntityFrom(source, 8 + TridentEnchantments.impalingDamage(trident, target))) {
            if (target instanceof EntityEnderman) return;
            if (target instanceof EntityLivingBase && shootingEntity instanceof EntityLivingBase) {
                EntityLivingBase victim = (EntityLivingBase) target;
                EnchantmentHelper.applyThornEnchantments(victim, shootingEntity);
                EnchantmentHelper.applyArthropodEnchantments((EntityLivingBase) shootingEntity, victim);
            }
        }
        motionX *= -0.01;
        motionY *= -0.1;
        motionZ *= -0.01;
        velocityChanged = true;
        BlockPos strike = target.getPosition();
        if (world.isThundering() && world.canSeeSky(strike)
                && EnchantmentHelper.getEnchantmentLevel(TridentEnchantments.CHANNELING, trident) > 0) {
            EntityLightningBolt bolt = shootingEntity instanceof EntityPlayerMP
                    ? new ChanneledLightningBolt(world, strike, (EntityPlayerMP) shootingEntity)
                    : new EntityLightningBolt(world, strike.getX() + 0.5, strike.getY(), strike.getZ() + 0.5, false);
            world.addWeatherEffect(bolt);
            playSound(PSSounds.TRIDENT_THUNDER, 5, 1);
        } else {
            playSound(PSSounds.TRIDENT_HIT, 1, 1);
        }
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        if (world.isRemote || isDead || shootingEntity != null && !player.getUniqueID().equals(ownerId)) return;
        if (dataManager.get(RETURNING)) {
            boolean accepted = pickupStatus == PickupStatus.CREATIVE_ONLY && player.capabilities.isCreativeMode
                    || pickupStatus == PickupStatus.ALLOWED && player.inventory.addItemStackToInventory(getArrowStack());
            if (accepted) {
                player.onItemPickup(this, 1);
                setDead();
            }
        } else {
            super.onCollideWithPlayer(player);
        }
    }

    @Override
    protected ItemStack getArrowStack() {
        return trident.copy();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setTag("Trident", trident.writeToNBT(new NBTTagCompound()));
        tag.setBoolean("DealtDamage", dataManager.get(DEALT_DAMAGE));
        tag.setBoolean("Returning", dataManager.get(RETURNING));
        if (ownerId != null) tag.setUniqueId("Owner", ownerId);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Trident")) trident = new ItemStack(tag.getCompoundTag("Trident"));
        dataManager.set(DEALT_DAMAGE, tag.getBoolean("DealtDamage"));
        dataManager.set(RETURNING, tag.getBoolean("Returning"));
        ownerId = tag.hasUniqueId("Owner") ? tag.getUniqueId("Owner") : null;
        syncItem();
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(shootingEntity == null ? -1 : shootingEntity.getEntityId());
        buffer.writeDouble(motionX).writeDouble(motionY).writeDouble(motionZ);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        ownerEntityId = buffer.readInt();
        shootingEntity = world.getEntityByID(ownerEntityId);
        setVelocity(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void despawnEmbedded(EntityArrow arrow) {
        if (!(arrow instanceof ThrownTridentEntity) || arrow.pickupStatus != PickupStatus.ALLOWED
                || ((ThrownTridentEntity) arrow).dataManager.get(LOYALTY) == 0) arrow.setDead();
    }

    public static float waterDrag(float normal, EntityArrow arrow) {
        return arrow instanceof ThrownTridentEntity ? 0.99F : normal;
    }

    public static SoundEvent groundSound(SoundEvent normal, EntityArrow arrow) {
        return arrow instanceof ThrownTridentEntity ? PSSounds.TRIDENT_HIT_GROUND : normal;
    }
}
