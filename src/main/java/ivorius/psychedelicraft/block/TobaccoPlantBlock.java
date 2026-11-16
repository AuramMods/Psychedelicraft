/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */
package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;

public class TobaccoPlantBlock extends CannabisPlantBlock {
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public TobaccoPlantBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(TOP, false));
    }


    @Override
    public BlockState getStateForHeight(int y) {
        return defaultBlockState().setValue(TOP, y > 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TOP);
    }

    @Override public int getMaxAge() { return 7; }

    @Override
    public IntegerProperty getAgeProperty() {
        return BlockStateProperties.AGE_7;
    }

    @Override
    public int getMaxHeight() {
        return 2;
    }

    @Override
    public int getMaxAge(BlockState state) {
        return 7;
    }

    @Override
    protected float getRandomGrowthChance() {
        return 0.1F;
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.FARMLAND) || floor.is(this) || floor.is(BlockTags.DIRT) || floor.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public void applyGrowth(Level world, BlockPos pos, BlockState state, boolean bonemeal) {
        int number = bonemeal ? world.random.nextInt(2) + 1 : 1;

        for (int i = 0; i < number; i++) {
            final boolean freeOver = world.isEmptyBlock(pos.above()) && getPlantSize(world, pos) < getMaxHeight();

            if (state.getValue(getAgeProperty()) < getMaxAge(state)) {
                state = state.cycle(getAgeProperty());
                world.setBlock(pos, state, Block.UPDATE_ALL);
            }
            if (freeOver && state.getValue(getAgeProperty()) >= getMaxAge(state)) {
                pos = pos.above();
                state = defaultBlockState().setValue(TOP, true);
                world.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean client) {
        return (world.isEmptyBlock(pos.above()) && getPlantSize(world, pos) < getMaxHeight())
                || state.getValue(getAgeProperty()) < getMaxAge(state);
    }
}
