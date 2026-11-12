package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.JuniperSignBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

final class JuniperSignItem extends Item {
    JuniperSignItem() {
        setMaxStackSize(16);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos clicked, EnumHand hand,
                                     EnumFacing facing, float x, float y, float z) {
        if (facing == EnumFacing.DOWN) {
            return EnumActionResult.FAIL;
        }
        boolean replaceable = world.getBlockState(clicked).getBlock().isReplaceable(world, clicked);
        BlockPos pos = replaceable ? clicked : clicked.offset(facing);
        ItemStack held = player.getHeldItem(hand);
        boolean standing = facing == EnumFacing.UP;
        JuniperSignBlock block = standing ? PSBlocks.JUNIPER_SIGN : PSBlocks.JUNIPER_WALL_SIGN;
        IBlockState state = standing
                ? block.getDefaultState().withProperty(JuniperSignBlock.ROTATION,
                    MathHelper.floor((player.rotationYaw + 180F) * 16F / 360F + 0.5F) & 15)
                : block.getDefaultState().withProperty(JuniperSignBlock.FACING, facing);
        if (!player.canPlayerEdit(pos, facing, held) || !world.isBlockModifiable(player, pos)
                || !world.mayPlace(block, pos, false, facing, player) || !block.canStay(world, pos, state)) {
            return EnumActionResult.FAIL;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!world.setBlockState(pos, state, 11)) {
            return EnumActionResult.FAIL;
        }
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            JuniperSignBlockEntity sign = JuniperSignBlockEntity.get(world, pos);
            if (sign != null && !ItemBlock.setTileEntityNBT(world, player, pos, held)) {
                sign.queuePlacementEditor(serverPlayer);
            }
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, held);
        }
        SoundType sound = block.getSoundType(state, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1F) * 0.5F, sound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
