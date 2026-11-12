/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.LatticeBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.SoundType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

final class WineGrapesItem extends ItemFood {
    WineGrapesItem() {
        super(1, 0.5F, true);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 15;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                     EnumFacing face, float x, float y, float z) {
        if (player.isSneaking() || !world.isBlockModifiable(player, pos)
                || world.getBlockState(pos).getBlock() != PSBlocks.LATTICE) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote && LatticeBlock.growOnto(world, pos, PSBlocks.WINE_GRAPE_LATTICE)) {
            world.playSound(null, pos, SoundType.PLANT.getHitSound(), SoundCategory.BLOCKS, 1F, 1F);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        if (user instanceof EntityPlayer && ((EntityPlayer) user).isCreative()) {
            super.onItemUseFinish(stack.copy(), world, user);
            return stack;
        }
        return super.onItemUseFinish(stack, world, user);
    }
}
