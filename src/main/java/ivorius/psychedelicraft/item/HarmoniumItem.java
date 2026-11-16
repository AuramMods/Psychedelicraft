/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class HarmoniumItem extends Item {
    public HarmoniumItem(){super(new Item.Properties());}
    public static boolean hasColor(ItemStack stack){CompoundTag display=stack.getTagElement("display");return display!=null&&display.contains("color",Tag.TAG_ANY_NUMERIC);}
    public static int getColor(ItemStack stack){return hasColor(stack)?stack.getTagElement("display").getInt("color")&0xffffff:0xa06540;}
    public static void setColor(ItemStack stack,int color){stack.getOrCreateTagElement("display").putInt("color",color&0xffffff);}
    public static int packColor(float[] color){return (int)(color[0]*255)<<16|(int)(color[1]*255)<<8|(int)(color[2]*255);}
}
