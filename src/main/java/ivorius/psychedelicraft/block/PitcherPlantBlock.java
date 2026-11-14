package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

import static net.minecraft.block.BlockDoublePlant.EnumBlockHalf.LOWER;
import static net.minecraft.block.BlockDoublePlant.EnumBlockHalf.UPPER;
import static net.minecraft.block.BlockDoublePlant.HALF;

final class PitcherPlantBlock extends BlockBush {
    PitcherPlantBlock() {
        setDefaultState(blockState.getBaseState().withProperty(HALF, LOWER));
        setSoundType(SoundType.PLANT);
        setTickRandomly(false);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(HALF, (metadata & 8) == 0 ? LOWER : UPPER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HALF) == LOWER ? 0 : 8;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return state.getBlock() == Blocks.MYCELIUM || super.canSustainBush(state);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        BlockPos above = pos.up();
        return pos.getY() > 0 && above.getY() < world.getHeight() && world.getBlockState(above).getBlock().isReplaceable(world, above)
                && world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                && canSustainBush(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        if (state.getValue(HALF) == UPPER) {
            IBlockState below = world.getBlockState(pos.down());
            return below.getBlock() == this && below.getValue(HALF) == LOWER;
        }
        IBlockState above = world.getBlockState(pos.up());
        return above.getBlock() == this && above.getValue(HALF) == UPPER && canSustainBush(world.getBlockState(pos.down()));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos.up(), state.withProperty(HALF, UPPER), 3);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        IBlockState below = world.getBlockState(pos.down());
        if (player.capabilities.isCreativeMode && state.getValue(HALF) == UPPER
                && below.getBlock() == this && below.getValue(HALF) == LOWER) {
            world.setBlockState(pos.down(), Blocks.AIR.getDefaultState(), 2);
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return state.getValue(HALF) == UPPER ? Items.AIR : super.getItemDropped(state, random, fortune);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return FULL_BLOCK_AABB.offset(state.getOffset(world, state.getValue(HALF) == UPPER ? pos.down() : pos));
    }
}
