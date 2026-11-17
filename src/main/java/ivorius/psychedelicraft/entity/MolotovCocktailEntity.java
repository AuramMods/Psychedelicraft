/* Copyright (c) 2014, Lukas Tenbrink. Native projectile integration adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.entity;
import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.fluid.BrewProfile;
import ivorius.psychedelicraft.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public final class MolotovCocktailEntity extends ThrowableItemProjectile implements IEntityAdditionalSpawnData {
    private int spawnOwnerId=-1;
    private java.util.UUID spawnOwnerUuid;
    public MolotovCocktailEntity(EntityType<? extends MolotovCocktailEntity> type,Level level){super(type,level);setItem(ItemStack.EMPTY);}
    public MolotovCocktailEntity(Level level,LivingEntity owner,ItemStack stack){super(PSEntities.MOLOTOV_COCKTAIL.get(),owner,level);setItem(stack);}
    @Override protected Item getDefaultItem(){return PSItems.MOLOTOV_COCKTAIL.get();}
    @Override public void setItem(ItemStack stack){var item=stack.is(PSItems.MOLOTOV_COCKTAIL.get())?stack.copy():new ItemStack(PSItems.MOLOTOV_COCKTAIL.get());item.setCount(1);item.getOrCreateTag().putBoolean(MolotovCocktailItem.FLYING,true);super.setItem(item);}
    @Override public ItemStack getItem(){return super.getItem().copy();}
    @Override public Entity getOwner(){
        var owner=super.getOwner();
        // Chunk entities may reach a newly connected observer before the owner's player/entity packet.
        if(owner==null&&spawnOwnerUuid!=null){
            var candidate=spawnOwnerId<0?null:level().getEntity(spawnOwnerId);
            if(candidate==null||!candidate.getUUID().equals(spawnOwnerUuid))candidate=level().getPlayerByUUID(spawnOwnerUuid);
            if(candidate==null&&level() instanceof ServerLevel server)candidate=server.getEntity(spawnOwnerUuid);
            if(candidate!=null&&!candidate.isRemoved()&&candidate.getUUID().equals(spawnOwnerUuid)){setOwner(candidate);owner=candidate;}
        }
        return owner;
    }
    public float strength(){return BrewProfile.projectileStrength(PSItems.MOLOTOV_COCKTAIL.get().contents(getItem()));}
    @Override public void tick(){super.tick();if(level().isClientSide&&!isRemoved()&&strength()>0)particles(1);}
    @Override protected void onHitEntity(EntityHitResult hit){
        if(level().isClientSide||isRemoved())return;
        float blast=strength()*.6F,fire=strength()*2;damage(hit.getEntity(),blast,fire,1);
        if(blast>0)for(var nearby:level().getEntities(this,getBoundingBox().inflate(blast))){if(nearby==hit.getEntity())continue;float distance=distanceTo(nearby);if(distance<blast)damage(nearby,blast,fire,1-distance/blast);}
    }
    private void damage(Entity target,float blast,float fire,float scale){
        target.hurt(PSDamageTypes.molotov(this,getOwner(),target,getItem()),scale*Math.max(4,blast*.6F+fire*.3F));
        if(fire>0)target.setRemainingFireTicks((int)Math.max(10,fire*3));
    }
    @Override protected void onHit(HitResult hit){
        if(level().isClientSide||isRemoved())return;super.onHit(hit);if(isRemoved())return;
        float blast=strength()*.6F,fire=strength()*2;
        playSound(SoundEvents.GLASS_BREAK,1,1);
        for(int count=Mth.ceil(fire*2);count>0;count--)particles(fire);
        if(blast>0)level().explode(this,level().damageSources().thrown(this,getOwner()),new ExplosionDamageCalculator(){
            @Override public boolean shouldBlockExplode(Explosion explosion,BlockGetter world,BlockPos pos,BlockState state,float power){return state.canBeReplaced();}
        },getX(),getY(),getZ(),blast,fire>0,Level.ExplosionInteraction.MOB);
        else playSound(SoundEvents.FIRE_EXTINGUISH,1,1);
        discard();
    }
    private void particles(float spread){particle(ParticleTypes.FLAME,getY(),spread);particle(ParticleTypes.LAVA,getY()+getBbHeight(),spread);}
    private void particle(SimpleParticleType type,double y,float spread){
        double x=getX()+random.triangle(0,.5*spread),z=getZ()+random.triangle(0,.5*spread);y+=random.triangle(0,.5*spread);
        if(level().isClientSide)level().addParticle(type,x,y,z,0,0,0);else((ServerLevel)level()).sendParticles(type,x,y,z,1,0,0,0,0);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void writeSpawnData(FriendlyByteBuf buffer){
        var owner=getOwner();var uuid=owner==null?null:owner.getUUID();
        if(uuid==null){
            // Native saved ownership still exists when a chunk is tracked before its player reconnects.
            var saved=new net.minecraft.nbt.CompoundTag();super.addAdditionalSaveData(saved);
            if(saved.hasUUID("Owner"))uuid=saved.getUUID("Owner");
        }
        buffer.writeInt(owner==null?-1:owner.getId());buffer.writeBoolean(uuid!=null);if(uuid!=null)buffer.writeUUID(uuid);
    }
    @Override public void readSpawnData(FriendlyByteBuf buffer){spawnOwnerId=buffer.readInt();spawnOwnerUuid=buffer.readBoolean()?buffer.readUUID():null;getOwner();}
}
