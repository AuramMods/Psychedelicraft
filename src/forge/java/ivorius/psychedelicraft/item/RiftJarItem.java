/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public final class RiftJarItem extends ItemBlock {
    private static final String FILL = "riftFraction";

    RiftJarItem(Block block) {
        super(block);
    }

    public ItemStack filled(float amount) {
        ItemStack stack = new ItemStack(this);
        if (amount > 0) stack.setTagInfo(FILL, new NBTTagFloat(amount));
        return stack;
    }

    public static float getRiftFraction(ItemStack stack) {
        float amount = stack.hasTagCompound() ? stack.getTagCompound().getFloat(FILL) : 0;
        return Float.isFinite(amount) ? Math.max(amount, 0) : 0;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (isInCreativeTab(tab) && PSConfig.balancing.enableRiftJars) {
            for (float amount : new float[]{0, 0.25F, 0.55F, 0.75F, 0.9F}) stacks.add(filled(amount));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal(getUnlocalizedName() + "." + fillName(getRiftFraction(stack))));
    }

    private static String fillName(float amount) {
        if (amount <= 0) return "empty";
        if (amount < 0.4F) return "slightly_filled";
        if (amount < 0.6F) return "half_filled";
        if (amount < 0.8F) return "filled";
        return "over_filled";
    }
}
