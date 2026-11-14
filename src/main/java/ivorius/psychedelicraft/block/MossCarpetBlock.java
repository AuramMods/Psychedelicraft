package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

final class MossCarpetBlock extends Block {
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0D, 0D, 0D, 1D, 1D / 16D, 1D);

    MossCarpetBlock() {
        super(Material.CARPET, MapColor.GREEN);
        setHardness(.1F);
        setSoundType(SoundType.PLANT);
        setLightOpacity(0);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return !world.isAirBlock(pos.down()) && super.canPlaceBlockAt(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!world.isRemote && world.isAirBlock(pos.down())) world.destroyBlock(pos, true);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
