/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;

public final class HarmoniumItem extends Item {
    private static final int DEFAULT_COLOR = 0xa06540;

    public static boolean hasColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound("display");
        return display != null && display.hasKey("color", Constants.NBT.TAG_ANY_NUMERIC);
    }

    public static int getColor(ItemStack stack) {
        return hasColor(stack) ? stack.getSubCompound("display").getInteger("color") & 0xffffff : DEFAULT_COLOR;
    }

    public static void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound("display").setInteger("color", color & 0xffffff);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stacks) {
        if (!PSConfig.balancing.enableHarmonium || !isInCreativeTab(tab)) return;
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            ItemStack stack = new ItemStack(this);
            float[] components = dye.getColorComponentValues();
            setColor(stack, packColor(components));
            stacks.add(stack);
        }
    }

    public static int packColor(float[] color) {
        return (int) (color[0] * 255) << 16 | (int) (color[1] * 255) << 8 | (int) (color[2] * 255);
    }
}
