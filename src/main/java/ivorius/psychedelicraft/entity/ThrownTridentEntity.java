package ivorius.psychedelicraft.entity;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ThrownTridentEntity extends EntityArrow implements IEntityAdditionalSpawnData {
    private static final DataParameter<Boolean> FOIL = EntityDataManager.createKey(ThrownTridentEntity.class, DataSerializers.BOOLEAN);
    private ItemStack trident;
    private boolean dealtDamage;
    private UUID ownerId;

    public ThrownTridentEntity(World world) {
        super(world);
        trident = new ItemStack(PSItems.TRIDENT);
    }

    public ThrownTridentEntity(World world, EntityLivingBase owner, ItemStack stack) {
        super(world, owner);
        ownerId = owner.getUniqueID();
        trident = stack.copy();
        dataManager.set(FOIL, stack.isItemEnchanted());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(FOIL, false);
    }

    public boolean hasFoil() {
        return dataManager.get(FOIL);
    }

    @Override
    public void onUpdate() {
        if (shootingEntity == null && ownerId != null && world instanceof WorldServer) {
            shootingEntity = ((WorldServer) world).getEntityFromUuid(ownerId);
        }
        if (timeInGround > 4) dealtDamage = true;
        super.onUpdate();
    }

    @Override
    @Nullable
    protected Entity findEntityOnPath(Vec3d start, Vec3d end) {
        return dealtDamage ? null : super.findEntityOnPath(start, end);
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        Entity target = hit.entityHit;
        if (target == null) {
            super.onHit(hit);
            return;
        }
        if (world.isRemote) return;
        dealtDamage = true;
        Entity attacker = shootingEntity == null ? this : shootingEntity;
        EntityDamageSourceIndirect source = new EntityDamageSourceIndirect("trident", this, attacker);
        source.setProjectile();
        if (target.attackEntityFrom(source, 8)) {
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
        playSound(PSSounds.TRIDENT_HIT, 1, 1);
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        if (shootingEntity == null || player.getUniqueID().equals(ownerId)) {
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
        tag.setBoolean("DealtDamage", dealtDamage);
        if (ownerId != null) tag.setUniqueId("Owner", ownerId);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Trident")) trident = new ItemStack(tag.getCompoundTag("Trident"));
        dealtDamage = tag.getBoolean("DealtDamage");
        ownerId = tag.hasUniqueId("Owner") ? tag.getUniqueId("Owner") : null;
        dataManager.set(FOIL, trident.isItemEnchanted());
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(shootingEntity == null ? -1 : shootingEntity.getEntityId());
        buffer.writeDouble(motionX).writeDouble(motionY).writeDouble(motionZ);
        buffer.writeBoolean(dealtDamage);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        shootingEntity = world.getEntityByID(buffer.readInt());
        setVelocity(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        dealtDamage = buffer.readBoolean();
    }

    public static float waterDrag(float normal, EntityArrow arrow) {
        return arrow instanceof ThrownTridentEntity ? 0.99F : normal;
    }

    public static SoundEvent groundSound(SoundEvent normal, EntityArrow arrow) {
        return arrow instanceof ThrownTridentEntity ? PSSounds.TRIDENT_HIT_GROUND : normal;
    }
}
