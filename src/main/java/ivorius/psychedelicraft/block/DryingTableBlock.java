/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.PSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public final class DryingTableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,12,16);
    public DryingTableBlock(BlockBehaviour.Properties properties){super(properties.noOcclusion());}
    @Override public int getFlammability(BlockState state,BlockGetter world,BlockPos pos,Direction side){return state.is(PSBlocks.DRYING_TABLE.get())?20:0;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter world,BlockPos pos,Direction side){return state.is(PSBlocks.DRYING_TABLE.get())?5:0;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DryingTableBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world,BlockState state,BlockEntityType<T> type){
        return world.isClientSide?null:createTickerHelper(type,PSBlockEntities.DRYING_TABLE.get(),(level,pos,current,table)->table.tick((ServerLevel)level));
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(world.getBlockEntity(pos) instanceof DryingTableBlockEntity table))return InteractionResult.FAIL;
        if(player instanceof ServerPlayer serverPlayer)NetworkHooks.openScreen(serverPlayer,table,pos);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())){
            if(!world.isClientSide&&world.getBlockEntity(pos) instanceof DryingTableBlockEntity table){
                Containers.dropContents(world,pos,table);world.updateNeighbourForOutputSignal(pos,this);
            }
            super.onRemove(state,world,pos,replacement,moving);
        }
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level world,BlockPos pos){
        return world.getBlockEntity(pos) instanceof DryingTableBlockEntity table?(int)(table.getHeatRatio()*15):0;
    }
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter world,BlockPos pos,Direction side){
        return world.getBlockEntity(pos) instanceof DryingTableBlockEntity table?(int)(table.getDryingProgress()*15):0;
    }
}
