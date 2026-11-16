/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.level.block.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public class CocaPlantBlock extends CannabisPlantBlock {
    private static final IntegerProperty AGE_12 = IntegerProperty.create("age", 0, 12);

    public CocaPlantBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override public int getMaxAge() { return 12; }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof CannabisPlantBlock) return null;
        return super.getStateForPlacement(context);
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE_12;
    }

    @Override
    public int getMaxAge(BlockState state) {
        return 12;
    }

    @Override
    protected float getRandomGrowthChance() {
        return 0.1F;
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.FARMLAND) || floor.is(this) || floor.is(BlockTags.DIRT) || floor.is(Blocks.GRASS_BLOCK);
    }
}
