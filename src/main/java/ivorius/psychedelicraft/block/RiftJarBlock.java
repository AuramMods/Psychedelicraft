/* Copyright (c) 2014, Lukas Tenbrink. Native shape/integration adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.sounds.SoundSource;
import java.util.*;

public final class RiftJarBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE=Shapes.or(box(4,0,4,12,5,12),box(4.5,5,4.5,11.5,7,11.5),box(4,7,4,12,12,12),box(5,12,5,11,14,11));
    public RiftJarBlock(){super(Properties.of().strength(.5F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.DESTROY));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RiftJarBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,PSBlockEntities.RIFT_JAR.get(),RiftJarBlockEntity::tick);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof RiftJarBlockEntity jar)jar.fromItem(stack);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof RiftJarBlockEntity jar))return InteractionResult.FAIL;
        if(!level.isClientSide){var sound=player.isShiftKeyDown()?PSSounds.RIFT_JAR_TOGGLE:jar.toggleLid()?PSSounds.RIFT_JAR_OPEN:PSSounds.RIFT_JAR_CLOSE;if(player.isShiftKeyDown())jar.toggleMode();level.playSound(null,pos,sound.get(),SoundSource.BLOCKS,1,1);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    // Native survival harvesting passes the cached block entity after removing it from the world.
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof RiftJarBlockEntity jar&&!jar.isBroken()?List.of(jar.portableStack()):List.of();}
}
