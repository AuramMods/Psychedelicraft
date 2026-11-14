package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

final class SweetBerriesItem extends QuickFoodItem {
    private final ItemBlock planter = new ItemBlock(PSBlocks.SWEET_BERRY_BUSH);

    SweetBerriesItem() {
        super(2, 0.1F, false, 32);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing face, float x, float y, float z) {
        EnumActionResult planted = planter.onItemUse(player, world, pos, hand, face, x, y, z);
        return planted == EnumActionResult.SUCCESS ? planted : onItemRightClick(world, player, hand).getType();
    }
}
