package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

final class SmallFlowerBlock extends BlockBush {
    SmallFlowerBlock() {
        setSoundType(SoundType.PLANT);
        setTickRandomly(false);
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return state.getBlock() == Blocks.MYCELIUM || super.canSustainBush(state);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return canSustainBush(world.getBlockState(pos.down()));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BUSH_AABB.offset(state.getOffset(world, pos));
    }
}
