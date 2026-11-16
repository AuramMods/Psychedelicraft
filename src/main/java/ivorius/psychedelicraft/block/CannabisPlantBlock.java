/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;

public class CannabisPlantBlock extends CropBlock {
    public static final int MAX_AGE = 15;
    public static final int MAX_AGE_WHILE_COVERED = 11;

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public static final BooleanProperty GROWING = BooleanProperty.create("growing");
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    public CannabisPlantBlock(BlockBehaviour.Properties settings) {
        super(settings.randomTicks().noOcclusion());
        registerDefaultState(defaultBlockState().setValue(NATURAL, false).setValue(GROWING, true));
    }

    public BlockState getStateForHeight(int y) {
        return defaultBlockState();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return BlockStateProperties.AGE_15;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(getAgeProperty(), GROWING, NATURAL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public int getMaxAge() { return MAX_AGE; }

    public int getMaxAge(BlockState state) {
        return MAX_AGE;
    }

    public int getMaxHeight() {
        return 3;
    }

    protected float getRandomGrowthChance() {
        return 0.12F;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return asItem();
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(this) || super.mayPlaceOn(floor, world, pos);
    }

    @Override
    public final boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (state.getValue(NATURAL)) {
            BlockState floor = world.getBlockState(pos.below());
            return floor.is(this) || floor.is(Blocks.GRASS_BLOCK) || floor.is(BlockTags.DIRT);
        }
        BlockState floor = world.getBlockState(pos.below());
        return (world.getRawBrightness(pos, 0) >= 8 || world.canSeeSky(pos))
                && (mayPlaceOn(floor, world, pos.below())
                || floor.canSustainPlant(world, pos.below(), Direction.UP, this));
    }

    public boolean isMature(BlockState state) {
        return state.getValue(getAgeProperty()) >= getMaxAge(state) && !state.getValue(GROWING);
    }

    @Override public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean client) {
        return state.getValue(getAgeProperty()) < getMaxAge(state)
                || (world.isEmptyBlock(pos.above()) && getPlantSize(world, pos) < getMaxHeight());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (world.getRawBrightness(pos.above(), 0) >= 9 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(world, pos, state, random.nextFloat() < getRandomGrowthChance())) {
            if (isValidBonemealTarget(world, pos, state, false)) {
                applyGrowth(world, pos, state, false);
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(world, pos, state);
            }
        }
    }

    public void applyGrowth(Level world, BlockPos pos, BlockState state, boolean bonemeal) {
        int number = bonemeal ? world.random.nextInt(4) + 1 : 1;

        for (int i = 0; i < number; i++) {
            if (state.getValue(getAgeProperty()) < getMaxAge(state)) {
                state = state.cycle(getAgeProperty());
                world.setBlock(pos, state, Block.UPDATE_ALL);
            } else if (canGrowUpwards(world, pos, state)) {
                pos = pos.above();
                state = defaultBlockState();
                world.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
    }

    protected int getPlantSize(LevelReader world, BlockPos pos) {
        int plantSize = 1;
        while (world.getBlockState(pos.below(plantSize)).is(this)) {
            ++plantSize;
        }
        return plantSize;
    }

    protected boolean canGrowUpwards(Level world, BlockPos pos, BlockState state) {
        return world.isEmptyBlock(pos.above())
                && getPlantSize(world, pos) < getMaxHeight()
                && state.getValue(getAgeProperty()) > MAX_AGE_WHILE_COVERED;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        applyGrowth(world, pos, state, true);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(GROWING, neighborState.isAir() && getPlantSize(world, pos) < getMaxHeight());
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }
}
