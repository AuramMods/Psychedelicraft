/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public final class MashTubItem extends ItemBlock {
    public MashTubItem() {
        super(PSBlocks.MASH_TUB);
        setMaxStackSize(16);
    }

    public int getCapacity() {
        return MashTubBlockEntity.CAPACITY;
    }

    @Nullable
    private FluidStack contents(ItemStack stack) {
        NBTTagCompound data = stack.getSubCompound("fluid");
        return data == null ? null : FluidContents.read(data, getCapacity());
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return contents(stack) == null ? super.getItemStackLimit(stack) : 1;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid == null ? super.getItemStackDisplayName(stack)
                : I18n.translateToLocalFormatted("tile.psychedelicraft.mash_tub.filled",
                        I18n.translateToLocal(PSFluids.name(fluid)));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        FluidStack fluid = contents(stack);
        if (fluid != null || flag.isAdvanced()) {
            tooltip.add(I18n.translateToLocalFormatted("psychedelicraft.container.levels",
                    fluid == null ? 0 : fluid.amount, getCapacity()));
        }
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
