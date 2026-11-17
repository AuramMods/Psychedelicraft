/* Copyright (c) 2014, Lukas Tenbrink. Adapted from Sollace's native flower implementation. */
package ivorius.psychedelicraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class VineStemBlock extends FlowerBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final int MAX_AGE = 4;
    // Same clockwise square order as the accepted 1.12 implementation.
    private static final int[][] SPREAD = {{0,0},{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
    public VineStemBlock(Properties properties) {
        super(() -> MobEffects.DIG_SLOWDOWN, 5, properties.offsetType(OffsetType.XZ));
        registerDefaultState(defaultBlockState().setValue(AGE, 0).setValue(LatticeBlock.NORTH, false)
                .setValue(LatticeBlock.EAST, false).setValue(LatticeBlock.SOUTH, false).setValue(LatticeBlock.WEST, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, LatticeBlock.NORTH, LatticeBlock.SOUTH, LatticeBlock.EAST, LatticeBlock.WEST);
    }
    private static BooleanProperty property(Direction direction) {
        return switch(direction) { case NORTH -> LatticeBlock.NORTH; case SOUTH -> LatticeBlock.SOUTH; case EAST -> LatticeBlock.EAST; case WEST -> LatticeBlock.WEST; default -> throw new IllegalArgumentException("Horizontal direction required"); };
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) for (Direction direction : Direction.Plane.HORIZONTAL)
            state = state.setValue(property(direction), context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(PSBlocks.MORNING_GLORY_LATTICE.get()));
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction.getAxis().isHorizontal()) state = state.setValue(property(direction), neighbour.is(PSBlocks.MORNING_GLORY_LATTICE.get()));
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) { level.destroyBlock(pos, true); return; }
        if (state.getValue(AGE) < MAX_AGE) level.setBlock(pos, state.cycle(AGE), Block.UPDATE_ALL);
        for (int[] offset : SPREAD) if (LatticeBlock.growOnto(level, pos.offset(offset[0], 0, offset[1]), PSBlocks.MORNING_GLORY_LATTICE.get())) return;
    }
}
