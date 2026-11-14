package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

final class HangingRootsBlock extends BlockBush implements IShearable {
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(.125D, .25D, .125D, .875D, 1D, .875D);

    HangingRootsBlock() {
        setSoundType(SoundType.PLANT);
        setTickRandomly(false);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return world.getBlockState(pos.up()).isSideSolid(world, pos.up(), EnumFacing.DOWN);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean isShearable(ItemStack tool, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack tool, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }
}
