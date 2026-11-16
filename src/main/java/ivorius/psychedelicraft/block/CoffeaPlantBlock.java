/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.level.block.*;

public class CoffeaPlantBlock extends TobaccoPlantBlock {

    public CoffeaPlantBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public int getMaxAge(BlockState state) {
        return state.getValue(TOP) ? 3 : super.getMaxAge(state);
    }
}
