package ivorius.psychedelicraft.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

public final class DryingRecipe {
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT = 0;
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;

    private DryingRecipe(String path, Item input, Item output) {
        this.id = Psychedelicraft.id("drying_table/" + path);
        this.input = Ingredient.fromItems(input);
        this.output = new ItemStack(output, 3);
    }

    public ResourceLocation getId() {
        return id;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public static List<DryingRecipe> recipes() {
        return Builtin.RECIPES;
    }

    @Nullable
    public static DryingRecipe match(IInventory inventory) {
        if (!inventory.getStackInSlot(OUTPUT).isEmpty()) {
            return null;
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
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getCount() != 1 || !input.apply(stack)) {
                return false;
            }
        }
        return true;
    }

    private static final class Builtin {
        private static final List<DryingRecipe> RECIPES = Collections.unmodifiableList(Arrays.asList(
                new DryingRecipe("brown_magic_mushrooms", Item.getItemFromBlock(Blocks.BROWN_MUSHROOM),
                        PSItems.BROWN_MAGIC_MUSHROOMS),
                new DryingRecipe("red_magic_mushrooms", Item.getItemFromBlock(Blocks.RED_MUSHROOM),
                        PSItems.RED_MAGIC_MUSHROOMS),
                new DryingRecipe("dried_cannabis_leaf", PSItems.CANNABIS_LEAF, PSItems.DRIED_CANNABIS_LEAF),
                new DryingRecipe("dried_cannabis_buds", PSItems.CANNABIS_BUDS, PSItems.DRIED_CANNABIS_BUDS),
                new DryingRecipe("tabacco_leaves", PSItems.TOBACCO_LEAVES, PSItems.DRIED_TOBACCO),
                new DryingRecipe("coca_leaves", PSItems.COCA_LEAVES, PSItems.DRIED_COCA_LEAVES)
        ));
    }
}
