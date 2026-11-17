/* Copyright (c) 2014, Lukas Tenbrink. Native tracking/particles adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

public final class RealityRiftEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE=SynchedEntityData.defineId(RealityRiftEntity.class,EntityDataSerializers.FLOAT),INSTABILITY=SynchedEntityData.defineId(RealityRiftEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CLOSING=SynchedEntityData.defineId(RealityRiftEntity.class,EntityDataSerializers.BOOLEAN);
    private float visualSize,previousVisualSize;
    public RealityRiftEntity(EntityType<? extends RealityRiftEntity> type,Level level){super(type,level);setNoGravity(true);if(!level.isClientSide)setRiftSize((float)random.triangle(.5,.5));}
    @Override protected void defineSynchedData(){entityData.define(SIZE,0F);entityData.define(INSTABILITY,0F);entityData.define(CLOSING,false);}
    public static boolean spawnNear(Entity source){var level=source.level();if(level.isClientSide)return false;var rift=PSEntities.REALITY_RIFT.get().create(level);rift.setPos(source.getX()+(level.random.nextDouble()-.5)*100,source.getY()+(level.random.nextDouble()-.5)*100,source.getZ()+(level.random.nextDouble()-.5)*100);return level.addFreshEntity(rift);}
    public float getRiftSize(){return entityData.get(SIZE);}
    public void setRiftSize(float value){entityData.set(SIZE,Float.isFinite(value)?Math.max(0,value):0);}
    public void addToRift(float value){setRiftSize(getRiftSize()+value);}
    public float getInstability(){return entityData.get(INSTABILITY);}
    public void setInstability(float value){entityData.set(INSTABILITY,Float.isFinite(value)?Mth.clamp(value,0,1):0);}
    public boolean isRiftClosing(){return entityData.get(CLOSING);}
    public void setRiftClosing(boolean value){entityData.set(CLOSING,value);}
    public boolean isCritical(){return !isRiftClosing()&&(getInstability()>0||getRiftSize()>3);}
    public float takeFromRift(float value){if(isCritical())return .2F;float before=getRiftSize();setRiftSize(before-value);return before-getRiftSize();}
    public float getVisualSize(float partial){return Mth.lerp(partial,previousVisualSize,visualSize);}
    @Override public AABB getBoundingBoxForCulling(){return getBoundingBox().inflate(20);}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override public Component getDisplayName(){return super.getDisplayName().copy().withStyle(style->style.withObfuscated(true));}
    @Override public void kill(){setRiftClosing(true);gameEvent(GameEvent.ENTITY_DIE);}
    @Override public void tick(){
        super.tick();setDeltaMovement(Vec3.ZERO);
        if(level().isClientSide)particles();else tickRift();
        previousVisualSize=visualSize;float target=getRiftSize(),step=visualSize+(target-visualSize)*.05F;visualSize=step<target?Math.min(target,step+.005F):Math.max(target,step-.005F);
        if(!level().isClientSide&&visualSize<=0&&target<=0)discard();
    }
    private void tickRift(){
        boolean critical=isCritical();float range=5+getInstability()*50;
        for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range),EntitySelector.NO_CREATIVE_OR_SPECTATOR)){
            double dose=(range-distanceTo(target))*.0005*getRiftSize();if(dose<=0)continue;
            if(target instanceof Player p){var drugs=DrugCapability.get(p);drugs.addToDrug(DrugType.ZERO,dose*20);drugs.addToDrug(DrugType.POWER,dose*200);}else if(critical)target.hurt(level().damageSources().magic(),(float)(dose*20));
        }
        if(critical){float before=getInstability(),after=Math.min(before+.001F,1);setInstability(after);replaceShell(before*50,after*50);}else setRiftSize(getRiftSize()-(isRiftClosing()?1F/20:1F/24000));
        if(getInstability()>=.9F)setRiftClosing(true);
    }
    private void replaceShell(float previous,float radius){
        var center=blockPosition();var cursor=new BlockPos.MutableBlockPos();double outer=(double)radius*radius,inner=(double)previous*previous;int extent=Mth.ceil(radius)-1;
        for(int x=-extent;x<=extent;x++)for(int y=-extent;y<=extent;y++){
            int row=x*x+y*y,worldY=center.getY()+y;if(row>=outer||worldY<level().getMinBuildHeight()||worldY>=level().getMaxBuildHeight())continue;
            int end=Mth.ceil(Math.sqrt(outer-row))-1;
            if(row>=inner)replaceRow(center,cursor,x,y,-end,end);else{int start=Mth.ceil(Math.sqrt(inner-row));replaceRow(center,cursor,x,y,-end,-start);replaceRow(center,cursor,x,y,start,end);}
        }
    }
    private void replaceRow(BlockPos center,BlockPos.MutableBlockPos cursor,int x,int y,int from,int to){for(int z=from;z<=to;z++){cursor.set(center.getX()+x,center.getY()+y,center.getZ()+z);if(level().hasChunkAt(cursor)&&!level().isEmptyBlock(cursor))level().setBlockAndUpdate(cursor,PSBlocks.GLITCH.get().defaultBlockState());}}
    private Vec3 particlePosition(){float distance=random.nextFloat()*random.nextFloat();return position().add((random.nextFloat()*8-4)*distance,(random.nextFloat()*8-4)*distance+getBbHeight()/2,(random.nextFloat()*8-4)*distance);}
    private void particles(){for(int i=random.nextInt(3);i>0;i--){var p=particlePosition();level().addParticle(ParticleTypes.LARGE_SMOKE,p.x,p.y,p.z,0,0,0);}for(int i=random.nextInt(2);i>0;i--){var p=particlePosition();level().addParticle(new DustParticleOptions(new Vector3f(1,.5F,.5F),1),p.x,p.y,p.z,-10,-10,-10);}float distance=random.nextFloat()*random.nextFloat();level().addParticle(ParticleTypes.ENCHANT,getX(),getY()+1+getBbHeight()/2,getZ(),(random.nextFloat()*8-4)*distance,(random.nextFloat()*8-4)*distance+getBbHeight()/2,(random.nextFloat()*8-4)*distance);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putFloat("riftSize",getRiftSize());tag.putFloat("instability",getInstability());tag.putBoolean("isRiftClosing",isRiftClosing());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){if(tag.contains("riftSize",Tag.TAG_ANY_NUMERIC))setRiftSize(tag.getFloat("riftSize"));if(tag.contains("instability",Tag.TAG_ANY_NUMERIC))setInstability(tag.getFloat("instability"));if(tag.contains("isRiftClosing",Tag.TAG_ANY_NUMERIC))setRiftClosing(tag.getBoolean("isRiftClosing"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
