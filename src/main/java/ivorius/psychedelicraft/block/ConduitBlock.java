package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.ConduitBlockEntity;
import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class ConduitBlock extends Block implements Waterloggable {
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D);

    ConduitBlock() {
        super(Material.GLASS);
        setHardness(3F);
        setSoundType(SoundType.GLASS);
        setLightLevel(1F);
        setLightOpacity(0);
    }

    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean hasOwnWaterTile() { return true; }
    @Override public boolean canPistonMove() { return false; }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new ConduitBlockEntity(); }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return SHAPE; }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        Waterlogging.scheduleIfWet(world, pos);
    }
}
