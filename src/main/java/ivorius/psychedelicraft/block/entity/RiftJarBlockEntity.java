/* Copyright (c) 2014, Lukas Tenbrink. Native lifecycle adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.entity.*;
import ivorius.psychedelicraft.entity.drug.*;
import ivorius.psychedelicraft.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;

public final class RiftJarBlockEntity extends BlockEntity {
    private float amount,lid,handle,previousLid,previousHandle,lastSentAmount;
    private boolean opening,sucking=true,broken;
    private int visualAge;
    private CompoundTag itemData=new CompoundTag();
    private final Map<UUID,Connection> connections=new HashMap<>();
    public RiftJarBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.RIFT_JAR.get(),pos,state);}
    public float getAmount(){return amount;}
    public boolean isBroken(){return broken;}
    public boolean isOpening(){return opening;}
    public boolean isSucking(){return sucking;}
    public float getLid(float partial){return net.minecraft.util.Mth.lerp(partial,previousLid,lid);}
    public float getHandle(float partial){return net.minecraft.util.Mth.lerp(partial,previousHandle,handle);}
    public int getVisualAge(){return visualAge;}
    public void setPreviewAge(int value){visualAge=value;}
    public Collection<Connection> getConnections(){return Collections.unmodifiableCollection(connections.values());}
    public void setAmount(float value){amount=finitePositive(value);publish();}
    public boolean toggleLid(){opening=!opening;publish();return opening;}
    public void toggleMode(){sucking=!sucking;publish();}
    public void fromItem(ItemStack stack){itemData=stack.hasTag()?stack.getTag().copy():new CompoundTag();itemData.remove(RiftJarItem.FILL);itemData.remove("BlockEntityTag");itemData.remove("BlockStateTag");setAmount(RiftJarItem.getRiftFraction(stack));}
    public ItemStack portableStack(){var stack=PSItems.RIFT_JAR.get().filled(amount);if(!itemData.isEmpty()){var tag=itemData.copy();if(stack.hasTag())tag.merge(stack.getTag());stack.setTag(tag);}return stack;}
    public static void tick(Level level,BlockPos pos,BlockState state,RiftJarBlockEntity jar){jar.tick();}
    private void tick(){
        previousLid=lid;previousHandle=handle;lid=opening?Math.min(1,lid+.02F):Math.max(0,lid-.02F);handle=sucking?Math.max(0,handle-.04F):Math.min(1,handle+.04F);visualAge++;
        if(level.isClientSide){tickConnections();return;}
        float before=amount;
        if(lid>0){if(sucking)collect();else if(amount>0)discharge();}
        if(amount>1){overload();return;}
        if(before!=amount||previousLid!=lid||previousHandle!=handle)setChanged();
        boolean emptied=before>0&&amount==0;if(emptied||lastSentAmount!=amount&&(amount==0||level.getGameTime()%5==0))publish();
    }
    private List<RealityRiftEntity> nearbyRifts(){return level.getEntitiesOfClass(RealityRiftEntity.class,new AABB(worldPosition.getX()-2,worldPosition.getY(),worldPosition.getZ()-2,worldPosition.getX()+3,worldPosition.getY()+10,worldPosition.getZ()+3),EntitySelector.ENTITY_STILL_ALIVE);}
    private void collect(){var rifts=nearbyRifts();if(rifts.isEmpty())return;float rate=(1F/rifts.size())*.001F*lid;for(var rift:rifts)amount+=rift.takeFromRift(rate);}
    private void discharge(){
        float drained=Math.min(.0004F*lid*amount+.0004F,amount);var center=worldPosition.getCenter();var area=new AABB(worldPosition.getX()-5,worldPosition.getY()-5,worldPosition.getZ()-2,worldPosition.getX()+6,worldPosition.getY()+6,worldPosition.getZ()+6);
        for(var player:level.getEntitiesOfClass(Player.class,area,EntitySelector.NO_CREATIVE_OR_SPECTATOR)){var drugs=DrugCapability.get(player);double effect=(5-player.position().distanceTo(center))*.2F*drained;drugs.addToDrug(DrugType.ZERO,effect*5);drugs.addToDrug(DrugType.POWER,effect*35);}
        amount-=drained;
    }
    private void overload(){
        var rifts=nearbyRifts();if(rifts.isEmpty()){var escaped=PSEntities.REALITY_RIFT.get().create(level);escaped.setPos(worldPosition.getX()+1,worldPosition.getY()+3.5,worldPosition.getZ()+1);escaped.setRiftSize(amount);if(!level.addFreshEntity(escaped))return;}else rifts.get(0).addToRift(amount);
        broken=true;amount=0;level.removeBlock(worldPosition,false);level.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,1,false,Level.ExplosionInteraction.BLOCK);
    }
    private void tickConnections(){
        if(sucking&&lid>0)for(var rift:nearbyRifts()){var connection=connections.computeIfAbsent(rift.getUUID(),key->new Connection(rift.getEyePosition()));connection.strength=Math.min(1,connection.strength+.02F*lid);}
        connections.values().removeIf(connection->(connection.strength-=.01F)<=0);
    }
    private void publish(){setChanged();if(level!=null&&!level.isClientSide){lastSentAmount=amount;var state=getBlockState();level.sendBlockUpdated(worldPosition,state,state,2);}}
    private static float finitePositive(float value){return Float.isFinite(value)?Math.max(0,value):0;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putFloat("currentRiftFraction",amount);tag.putBoolean("isOpening",opening);tag.putFloat("fractionOpen",lid);tag.putBoolean("suckingRifts",sucking);tag.putFloat("fractionHandleUp",handle);if(!itemData.isEmpty())tag.put("ItemData",itemData.copy());}
    @Override public void load(CompoundTag tag){super.load(tag);amount=finitePositive(tag.getFloat("currentRiftFraction"));opening=tag.getBoolean("isOpening");previousLid=lid=Math.min(1,finitePositive(tag.getFloat("fractionOpen")));sucking=!tag.contains("suckingRifts")||tag.getBoolean("suckingRifts");previousHandle=handle=Math.min(1,finitePositive(tag.getFloat("fractionHandleUp")));itemData=tag.getCompound("ItemData").copy();}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(3,10,3);}
    public static final class Connection {
        private final Vec3 position;private float strength;
        private Connection(Vec3 position){this.position=position;}
        public Vec3 getPosition(){return position;}public float getStrength(){return strength;}
    }
}
