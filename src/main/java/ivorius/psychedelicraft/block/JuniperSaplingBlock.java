/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.world.gen.JuniperTree;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.TerrainGen;

public final class JuniperSaplingBlock extends BlockBush implements IGrowable {
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.1, 0, 0.1, 0.9, 0.8, 0.9);

    JuniperSaplingBlock() {
        setDefaultState(blockState.getBaseState().withProperty(BlockSapling.STAGE, 0));
        setSoundType(SoundType.PLANT);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockSapling.STAGE);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(BlockSapling.STAGE, metadata & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockSapling.STAGE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        super.updateTick(world, pos, state, random);
        if (!world.isRemote && world.getBlockState(pos).getBlock() == this
                && world.getLightFromNeighbors(pos.up()) >= 9 && random.nextInt(7) == 0) {
            grow(world, random, pos, state);
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return random.nextFloat() < 0.45F;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        if (state.getValue(BlockSapling.STAGE) == 0) {
            world.setBlockState(pos, state.withProperty(BlockSapling.STAGE, 1), 4);
        } else if (TerrainGen.saplingGrowTree(world, random, pos)) {
            JuniperTree.generate(world, random, pos);
        }
    }
}
