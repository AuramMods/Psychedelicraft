/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper.ShapedPrimer;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nullable;

public final class BottleRecipe extends ShapedOreRecipe {
    // The donor uses sign colors. These are in stained glass metadata order.
    private static final int[] SIGN_COLORS = {
            0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD, 0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080,
            0xD3D3D3, 0x00FFFF, 0xA020F0, 0x0000FF, 0x8B4513, 0x00FF00, 0xFF0000, 0x000000
    };

    private BottleRecipe(@Nullable ResourceLocation group, ItemStack output, ShapedPrimer pattern) {
        super(group, output, pattern);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack glass = inventory.getStackInSlot(slot);
            if (glass.getItem() == Item.getItemFromBlock(Blocks.STAINED_GLASS)) {
                return coloredOutput(EnumDyeColor.byMetadata(glass.getMetadata()));
            }
        }
        return super.getCraftingResult(inventory);
    }

    public ItemStack coloredOutput(EnumDyeColor color) {
        ItemStack result = getRecipeOutput().copy();
        if (isDyeable(result)) setColor(result, SIGN_COLORS[color.getMetadata()]);
        return result;
    }

    static boolean isDyeable(ItemStack stack) {
        Item item = stack.getItem();
        return item == PSItems.BOTTLE || item == PSItems.MOLOTOV_COCKTAIL || item == PSItems.HARMONIUM
                || item instanceof ItemArmor && ((ItemArmor) item).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER;
    }

    static int getColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound("display");
        return display != null && display.hasKey("color", Constants.NBT.TAG_ANY_NUMERIC)
                ? display.getInteger("color") : 0xA06540;
    }

    static void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound("display").setInteger("color", color);
    }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);
            ShapedPrimer pattern = new ShapedPrimer();
            pattern.width = recipe.getRecipeWidth();
            pattern.height = recipe.getRecipeHeight();
            pattern.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
            pattern.input = recipe.getIngredients();
            String group = recipe.getGroup();
            return new BottleRecipe(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), pattern);
        }
    }
}
