/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DistilleryBlock extends FlaskBlock {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    DistilleryBlock() {
        super(PSGuiHandler.DISTILLERY, box(5, 0, 5, 11, 6, 11), box(4, 0, 6, 12, 5, 10),
                box(6, 0, 4, 10, 5, 12), box(6, 6, 6, 10, 14, 10),
                box(5, 9, 6, 11, 13, 10), box(6, 9, 5, 10, 13, 11));
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new DistilleryBlockEntity();
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(FACING, metadata < 6 ? EnumFacing.getFront(metadata) : EnumFacing.UP);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return withRotation(state, mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
                                           int metadata, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, findOutput(world, pos));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos changed) {
        if (world.isRemote) return;
        EnumFacing current = state.getValue(FACING);
        if (!current.getAxis().isHorizontal() || !canConnectTo(world, pos, current)) {
            EnumFacing output = findOutput(world, pos);
            if (output != current) world.setBlockState(pos, state.withProperty(FACING, output), 3);
        }
    }

    private static EnumFacing findOutput(World world, BlockPos pos) {
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            if (canConnectTo(world, pos, direction)) return direction;
        }
        return EnumFacing.UP;
    }

    public static boolean canConnectTo(World world, BlockPos pos, EnumFacing direction) {
        BlockPos destination = pos.offset(direction);
        if (!world.isBlockLoaded(destination)) return false;
        IBlockState state = world.getBlockState(destination);
        Block block = state.getBlock();
        if (block instanceof DistilleryBlock) return state.getValue(FACING) != direction.getOpposite();
        return block instanceof FlaskBlock || block instanceof BarrelBlock || block == PSBlocks.MASH_TUB_EDGE;
    }
}
