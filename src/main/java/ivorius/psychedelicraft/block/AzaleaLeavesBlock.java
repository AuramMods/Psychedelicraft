package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class AzaleaLeavesBlock extends BlockLeaves {
    AzaleaLeavesBlock() {
        setDefaultState(blockState.getBaseState().withProperty(DECAYABLE, true).withProperty(CHECK_DECAY, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DECAYABLE, CHECK_DECAY);
    }

    @Override
    public BlockPlanks.EnumType getWoodType(int meta) {
        return BlockPlanks.EnumType.OAK;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DECAYABLE, (meta & 4) == 0).withProperty(CHECK_DECAY, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(DECAYABLE) ? 0 : 4) | (state.getValue(CHECK_DECAY) ? 8 : 0);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos, state.withProperty(DECAYABLE, false), 2);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return this == PSBlocks.FLOWERING_AZALEA_LEAVES ? PSItems.FLOWERING_AZALEA : PSItems.AZALEA;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack tool, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }
}
