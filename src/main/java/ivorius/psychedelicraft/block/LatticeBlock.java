/* Copyright (c) 2014, Lukas Tenbrink. Adapted from Sollace's 1.20.1 lattice. */
package ivorius.psychedelicraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluids;

/** Native waterlogging, collision, rotation and piston state replace the 1.12 coremod. */
public class LatticeBlock extends CrossCollisionBlock {
    public LatticeBlock(Properties properties) {
        super(1.6F, 1.6F, 16, 16, 16, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, true).setValue(WEST, true).setValue(WATERLOGGED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, WATERLOGGED);
    }

    public static boolean connected(BlockState state, Direction direction) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(direction));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var pos = context.getClickedPos();
        var against = context.getLevel().getBlockState(pos.relative(context.getClickedFace().getOpposite()));
        BlockState state = context.getClickedFace().getAxis().isVertical() && against.getBlock() instanceof LatticeBlock
                ? copyStateProperties(defaultBlockState(), against) : connections(defaultBlockState(), context.getLevel(), pos);
        if (!hasConnections(state)) state = axis(state, context.getHorizontalDirection().getClockWise().getAxis());
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                           LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (direction.getAxis().isHorizontal()) {
            BlockState next = connections(state, level, pos);
            return hasConnections(next) ? next : axis(next, connected(state, Direction.NORTH) || connected(state, Direction.SOUTH)
                    ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    private static boolean hasConnections(BlockState state) {
        for (Direction direction : Direction.Plane.HORIZONTAL) if (connected(state, direction)) return true;
        return false;
    }

    private static BlockState axis(BlockState state, Direction.Axis axis) {
        for (Direction direction : Direction.Plane.HORIZONTAL) state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), direction.getAxis() == axis);
        return state;
    }

    private static BlockState connections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction), opposite = pos.relative(direction.getOpposite());
            BlockState neighbour = level.getBlockState(next);
            boolean connect = neighbour.getBlock() instanceof LatticeBlock || canAttach(level, next, neighbour, direction.getOpposite())
                    && canAttach(level, opposite, level.getBlockState(opposite), direction);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connect);
        }
        return state;
    }

    private static boolean canAttach(LevelAccessor level, BlockPos pos, BlockState state, Direction direction) {
        return state.getBlock() instanceof LatticeBlock
                || !Block.isExceptionForConnection(state) && state.isFaceSturdy(level, pos, direction)
                || state.getBlock() instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
    }

    public static BlockState copyStateProperties(BlockState target, BlockState source) {
        for (Direction direction : Direction.Plane.HORIZONTAL) target = target.setValue(PROPERTY_BY_DIRECTION.get(direction), connected(source, direction));
        return target.setValue(WATERLOGGED, source.getValue(WATERLOGGED));
    }

    public static boolean growOnto(Level level, BlockPos pos, LatticeCropBlock crop) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState previous = level.getBlockState(pos);
        return previous.is(PSBlocks.LATTICE.get()) && level.setBlock(pos, copyStateProperties(crop.defaultBlockState(), previous), Block.UPDATE_ALL);
    }
}
