/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.level.block.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public class HopPlantBlock extends CannabisPlantBlock {
    public HopPlantBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.FARMLAND) || floor.is(this) || floor.is(BlockTags.DIRT) || floor.is(Blocks.GRASS_BLOCK);
    }
}
