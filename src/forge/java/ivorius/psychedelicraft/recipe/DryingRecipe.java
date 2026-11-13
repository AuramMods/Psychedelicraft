package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class DryingRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT = 0;
    private final Ingredient input;
    private final ItemStack output;

    private DryingRecipe(Ingredient input, ItemStack output) {
        this.input = input;
        this.output = output;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public static List<DryingRecipe> recipes() {
        List<DryingRecipe> recipes = new ArrayList<>();
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            if (recipe instanceof DryingRecipe) recipes.add((DryingRecipe) recipe);
        }
        return recipes;
    }

    @Nullable
    public static DryingRecipe match(IInventory inventory) {
        if (!inventory.getStackInSlot(OUTPUT).isEmpty()) {
            return null;
        }
        for (int slot = 1; slot <= INPUT_COUNT; slot++) {
            if (inventory.getStackInSlot(slot).getCount() != 1) return null;
        }
        for (DryingRecipe recipe : recipes()) {
            if (recipe.matchesInputs(inventory)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean matchesInputs(IInventory inventory) {
        for (int slot = 1; slot <= INPUT_COUNT; slot++) {
            if (!input.apply(inventory.getStackInSlot(slot))) {
                return false;
            }
        }
        return true;
    }

    // Forge discovers machine recipes through the crafting registry. Only the table runs them.
    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return false;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return getOutput();
    }

    @Override
    public boolean canFit(int width, int height) {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            Ingredient input = CraftingHelper.getIngredient(json.get("ingredient"), context);
            ItemStack output = CraftingHelper.getItemStack(JsonUtils.getJsonObject(json, "result"), context);
            if (output.isEmpty() || output.getCount() > output.getMaxStackSize()) {
                throw new JsonSyntaxException("Drying output must fit in one stack");
            }
            return new DryingRecipe(input, output);
        }
    }
}
