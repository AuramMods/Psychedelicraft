/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MashTubItem extends FluidMachineItem {
    public MashTubItem() {
        super(PSBlocks.MASH_TUB, MashTubBlockEntity.CAPACITY);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing side, float x, float y, float z) {
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) pos = pos.offset(side);
        ItemStack stack = player.getHeldItem(hand);
        BlockPos center = null;
        for (int distance = 0; distance <= 2 && center == null; distance++) {
            for (int dx = -1; dx <= 1 && center == null; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = pos.add(dx, 0, dz);
                    if (Math.abs(dx) + Math.abs(dz) == distance && MashTubBlock.canPlaceBasin(world, candidate, player, side, stack)) {
                        center = candidate;
                        break;
                    }
                }
            }
        }
        if (center == null) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        IBlockState centerState = block.getDefaultState();
        if (!world.setBlockState(center, centerState, 3)) return EnumActionResult.FAIL;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && !world.setBlockState(center.add(dx, 0, dz),
                        PSBlocks.MASH_TUB_EDGE.edgeState(dx, dz), 3)) {
                    world.destroyBlock(center, false);
                    return EnumActionResult.FAIL;
                }
            }
        }
        block.onBlockPlacedBy(world, center, centerState, player, stack);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, center, stack);
        }
        SoundType sound = block.getSoundType(centerState, world, center, player);
        world.playSound(player, center, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
