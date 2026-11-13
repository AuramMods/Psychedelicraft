package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

final class JuniperSlabItem extends ItemSlab {
    JuniperSlabItem() {
        super(PSBlocks.JUNIPER_SLAB, PSBlocks.JUNIPER_SLAB, PSBlocks.JUNIPER_DOUBLE_SLAB);
        setHasSubtypes(false);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                float x, float y, float z, IBlockState state) {
        boolean wet = !world.isRemote && Waterlogging.isWaterSource(world.getBlockState(pos));
        if (!super.placeBlockAt(stack, player, world, pos, side, x, y, z, state)) return false;
        if (wet) Waterlogging.fillSource(world, pos);
        return true;
    }
}
