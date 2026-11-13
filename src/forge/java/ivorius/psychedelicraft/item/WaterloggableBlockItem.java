package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

class WaterloggableBlockItem extends ItemBlock {
    WaterloggableBlockItem(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                float hitX, float hitY, float hitZ, IBlockState state) {
        boolean wet = !world.isRemote && Waterlogging.isWaterSource(world.getBlockState(pos));
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, state)) return false;
        if (wet && Waterlogging.supports(world.getBlockState(pos))) Waterlogging.fillSource(world, pos);
        return true;
    }
}
