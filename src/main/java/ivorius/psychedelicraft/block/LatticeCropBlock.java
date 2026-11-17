/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.LatticeCropItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolActions;
import java.util.*;
import java.util.function.Supplier;

public final class LatticeCropBlock extends LatticeBlock implements BonemealableBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
    private final boolean requiresStem;
    private final int shearedAge;
    private final Supplier<Item> produce;

    public LatticeCropBlock(boolean requiresStem, int shearedAge, Supplier<Item> produce, Properties properties) {
        super(properties.randomTicks());
        this.requiresStem = requiresStem;
        this.shearedAge = shearedAge;
        this.produce = produce;
        registerDefaultState(defaultBlockState().setValue(AGE, 0).setValue(PERSISTENT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE, PERSISTENT);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(AGE, LatticeCropItem.age(context.getItemInHand())).setValue(PERSISTENT, true);
    }

    @Override public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        return state.getValue(AGE) > 0 ? SoundType.GRASS : SoundType.WOOD;
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack tool = player.getItemInHand(hand);
        if (!tool.canPerformAction(ToolActions.SHEARS_HARVEST) || state.getValue(AGE) < MAX_AGE || state.getValue(PERSISTENT)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(produce.get(), 3 + level.random.nextInt(2)));
            level.setBlock(pos, state.setValue(AGE, shearedAge), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1, 1);
            tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public boolean isRandomlyTicking(BlockState state) { return !state.getValue(PERSISTENT); }

    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PERSISTENT)) return;
        int age = state.getValue(AGE);
        if (!isRooted(level, state, pos)) {
            if (age > 0) level.setBlock(pos, state.setValue(AGE, age - 1), Block.UPDATE_ALL);
            return;
        }
        if (level.getRawBrightness(pos, 0) >= 9 && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt(35) == 0)) {
            if (age < MAX_AGE) level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_ALL);
            if (age > 0) spread(level, state, pos);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        return !state.getValue(PERSISTENT) && (state.getValue(AGE) < MAX_AGE || spreadTarget(level, state, pos) != null);
    }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (state.getValue(PERSISTENT)) return;
        if (state.getValue(AGE) < MAX_AGE) level.setBlock(pos, state.setValue(AGE, MAX_AGE), Block.UPDATE_ALL);
        else spread(level, state, pos);
    }
    private void spread(ServerLevel level, BlockState state, BlockPos pos) {
        BlockPos target = spreadTarget(level, state, pos);
        if (target != null) growOnto(level, target, this);
    }
    private static BlockPos spreadTarget(LevelReader level, BlockState state, BlockPos pos) {
        for (BlockPos next : linkedPositions(state, pos)) if (level.hasChunkAt(next) && level.getBlockState(next).is(PSBlocks.LATTICE.get())) return next;
        return null;
    }

    /** Breadth first search preserves the accepted twenty-level bound without recursive branch starvation. */
    public boolean isRooted(LevelReader level, BlockState state, BlockPos pos) {
        if (!requiresStem) return true;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(pos); queue.add(pos);
        for (int depth = 0; depth < 20 && !queue.isEmpty(); depth++) {
            int count = queue.size();
            for (int index = 0; index < count; index++) {
                BlockPos current = queue.remove();
                BlockState currentState = current.equals(pos) ? state : level.getBlockState(current);
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos side = current.relative(direction);
                    if (!connected(currentState, direction) && level.hasChunkAt(side) && level.getBlockState(side).is(PSBlocks.MORNING_GLORY.get())) return true;
                }
                if (depth + 1 >= 20) continue;
                for (BlockPos next : linkedPositions(currentState, current)) {
                    if (!visited.add(next) || !level.hasChunkAt(next)) continue;
                    BlockState neighbour = level.getBlockState(next);
                    if (neighbour.is(PSBlocks.MORNING_GLORY.get())) return true;
                    if (neighbour.is(this)) queue.add(next);
                }
            }
        }
        return false;
    }

    private static List<BlockPos> linkedPositions(BlockState state, BlockPos pos) {
        List<BlockPos> result = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) if (connected(state, direction)) {
            BlockPos side = pos.relative(direction);
            result.add(side.above()); result.add(side); result.add(side.below());
        }
        return result;
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return LatticeCropItem.withAge(new ItemStack(this), state.getValue(AGE));
    }
}
