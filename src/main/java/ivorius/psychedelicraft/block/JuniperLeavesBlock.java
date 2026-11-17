/* Copyright (c) 2014, Lukas Tenbrink. Native leaves adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class JuniperLeavesBlock extends LeavesBlock {
    public JuniperLeavesBlock(Properties properties){super(properties);}
    @Override public boolean isRandomlyTicking(BlockState state){return !state.getValue(PERSISTENT);}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.getValue(PERSISTENT))return;
        super.randomTick(state,level,pos,random);
        // Preserve the accepted donor order: the fruit roll follows decay and uses the original state.
        if(this==PSBlocks.JUNIPER_LEAVES.get()&&random.nextFloat()<.01F&&!state.getValue(WATERLOGGED))
            level.setBlock(pos,copyGrowth(PSBlocks.FRUITING_JUNIPER_LEAVES.get().defaultBlockState(),state),UPDATE_ALL);
    }
    public static BlockState copyGrowth(BlockState target,BlockState source){return target.setValue(DISTANCE,source.getValue(DISTANCE)).setValue(PERSISTENT,source.getValue(PERSISTENT)).setValue(WATERLOGGED,source.getValue(WATERLOGGED));}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        ItemStack held=player.getItemInHand(hand);
        if(this!=PSBlocks.FRUITING_JUNIPER_LEAVES.get()||held.getItem() instanceof BucketItem||held.is(PSItems.FILLED_BUCKET.get()))return InteractionResult.PASS;
        if(!level.isClientSide&&level.setBlock(pos,copyGrowth(PSBlocks.JUNIPER_LEAVES.get().defaultBlockState(),state),UPDATE_ALL))
            popResource(level,pos,new ItemStack(PSItems.JUNIPER_BERRIES.get()));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
