/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class FluidAwareShapelessRecipe extends ShapelessOreRecipe {
    FluidAwareShapelessRecipe(@Nullable ResourceLocation group, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(group, ingredients, result);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return RecipeMatcher.findMatches(occupiedStacks(inventory), input) != null;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        List<ItemStack> stacks = occupiedStacks(inventory);
        int[] assignment = RecipeMatcher.findMatches(stacks, input);
        NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        if (assignment == null) return remaining;

        int index = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Ingredient ingredient = input.get(assignment[index++]);
            if (ingredient instanceof FluidStackIngredient && ((FluidStackIngredient) ingredient).consumesFluid()) {
                remaining.set(slot, ((FluidStackIngredient) ingredient).drainedVessel(stack));
            } else {
                remaining.set(slot, ForgeHooks.getContainerItem(stack));
            }
        }
        return remaining;
    }

    private static List<ItemStack> occupiedStacks(InventoryCrafting inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (JsonElement entry : JsonUtils.getJsonArray(json, "ingredients")) {
                if (!entry.isJsonObject() || !entry.getAsJsonObject().has("fluid")) {
                    ingredients.add(CraftingHelper.getIngredient(entry, context));
                    continue;
                }
                JsonObject specification = entry.getAsJsonObject();
                Ingredient vessel = specification.entrySet().size() > 1 ? CraftingHelper.getIngredient(entry, context) : null;
                ingredients.add(FluidStackIngredient.parse(vessel, specification.get("fluid")));
            }
            if (ingredients.isEmpty() || ingredients.size() > 9) {
                throw new JsonSyntaxException("Expected one to nine shapeless ingredients");
            }
            String group = JsonUtils.getString(json, "group", "");
            ItemStack result = CraftingHelper.getItemStack(JsonUtils.getJsonObject(json, "result"), context);
            return new FluidAwareShapelessRecipe(group.isEmpty() ? null : new ResourceLocation(group), ingredients, result);
        }
    }
}
