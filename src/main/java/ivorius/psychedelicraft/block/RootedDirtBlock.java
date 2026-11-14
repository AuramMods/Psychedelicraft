package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

import java.util.Random;

final class RootedDirtBlock extends Block implements IGrowable {
    RootedDirtBlock() {
        super(Material.GROUND);
        setHardness(.5F);
        setSoundType(SoundType.GROUND);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        return world.isAirBlock(pos.down());
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        if (world.isAirBlock(pos.down())) world.setBlockState(pos.down(), PSBlocks.HANGING_ROOTS.getDefaultState(), 3);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plant) {
        return Blocks.DIRT.canSustainPlant(Blocks.DIRT.getDefaultState(), world, pos, direction, plant);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        ItemStack tool = player.getHeldItem(hand);
        if (!(tool.getItem() instanceof ItemHoe || tool.getItem().getToolClasses(tool).contains("hoe"))
                || !world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, face, tool)) return false;
        if (!world.isRemote && world.setBlockState(pos, Blocks.DIRT.getDefaultState(), 3)) {
            tool.damageItem(1, player);
            spawnAsEntity(world, pos.offset(face), new ItemStack(PSItems.HANGING_ROOTS));
            world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1F, 1F);
        }
        return true;
    }
}
