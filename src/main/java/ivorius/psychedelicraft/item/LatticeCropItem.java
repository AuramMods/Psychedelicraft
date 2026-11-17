/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import net.minecraft.nbt.Tag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

/** Native BlockStateTag carries the picked/silk-touched decorative growth stage. */
public final class LatticeCropItem extends BlockItem {
    public LatticeCropItem(Block block) { super(block, new Properties()); }
    public static int age(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(BLOCK_STATE_TAG, Tag.TAG_COMPOUND)) {
            try { return Math.max(0, Math.min(3, Integer.parseInt(stack.getTag().getCompound(BLOCK_STATE_TAG).getString("age")))); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }
    public static ItemStack withAge(ItemStack stack, int age) {
        stack.getOrCreateTagElement(BLOCK_STATE_TAG).putString("age", Integer.toString(Math.max(0, Math.min(3, age))));
        return stack;
    }
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) { return 700; }
}
