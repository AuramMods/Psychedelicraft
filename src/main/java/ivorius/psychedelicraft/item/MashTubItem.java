/* Copyright (c) 2014, Lukas Tenbrink. Native placement design by Sollace. */
package ivorius.psychedelicraft.item;
import ivorius.psychedelicraft.block.*;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.*;
public final class MashTubItem extends FluidMachineItem {
    public MashTubItem(){super(PSBlocks.MASH_TUB.get(),MashTubBlockEntity.CAPACITY);}
    public static boolean canPlaceBasin(BlockPlaceContext context,BlockPos center){
        Level world=context.getLevel();var player=context.getPlayer();if(world.isOutsideBuildHeight(center))return false;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))){
            if(!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||!world.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context,pos,context.getClickedFace()))||player!=null&&(!world.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())))return false;
            BlockState state=pos.equals(center)?PSBlocks.MASH_TUB.get().defaultBlockState():MashTubBlock.wallState(pos.getX()-center.getX(),pos.getZ()-center.getZ());
            if(!world.isUnobstructed(state,pos,player==null?CollisionContext.empty():CollisionContext.of(player)))return false;
        }return true;
    }
    @Override public InteractionResult place(BlockPlaceContext context){
        if(!context.canPlace())return InteractionResult.FAIL;BlockPos center=null;
        search:for(int distance=0;distance<=2;distance++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(Math.abs(dx)+Math.abs(dz)==distance){BlockPos candidate=context.getClickedPos().offset(dx,0,dz);if(canPlaceBasin(context,candidate)){center=candidate;break search;}}
        if(center==null)return InteractionResult.FAIL;Level world=context.getLevel();if(world.isClientSide)return InteractionResult.SUCCESS;
        Map<BlockPos,BlockState> previous=new LinkedHashMap<>();previous.put(center,world.getBlockState(center));
        for(BlockPos part:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))previous.putIfAbsent(part.immutable(),world.getBlockState(part));
        for(BlockPos part:previous.keySet()){
            BlockState state=part.equals(center)?PSBlocks.MASH_TUB.get().defaultBlockState():MashTubBlock.wallState(part.getX()-center.getX(),part.getZ()-center.getZ());
            if(!world.setBlock(part,state,Block.UPDATE_ALL)){for(var entry:previous.entrySet())world.setBlock(entry.getKey(),entry.getValue(),Block.UPDATE_ALL);return InteractionResult.FAIL;}
        }
        if(world.getBlockEntity(center) instanceof MashTubBlockEntity vat)vat.placedFrom(context.getItemInHand());
        var player=context.getPlayer();var held=context.getItemInHand();if(player instanceof ServerPlayer server)CriteriaTriggers.PLACED_BLOCK.trigger(server,center,held);
        var sound=getBlock().getSoundType(getBlock().defaultBlockState(),world,center,player);world.playSound(null,center,sound.getPlaceSound(),SoundSource.BLOCKS,(sound.getVolume()+1)/2,sound.getPitch()*.8F);
        world.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,center);if(player==null||!player.getAbilities().instabuild)held.shrink(1);return InteractionResult.CONSUME;
    }
}
