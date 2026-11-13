/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

abstract class SucculentPlantBlock extends AgePlantBlock {
    protected SucculentPlantBlock(int maxAge) {
        super(maxAge);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        BlockPos floor = pos.down();
        BlockFaceShape face = world.getBlockState(floor).getBlockFaceShape(world, floor, EnumFacing.UP);
        return face == BlockFaceShape.SOLID || face == BlockFaceShape.CENTER
                || face == BlockFaceShape.CENTER_SMALL || face == BlockFaceShape.CENTER_BIG;
    }
}
