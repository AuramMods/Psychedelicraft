/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class FillReceptacleRecipe extends ShapelessOreRecipe {
    private final Ingredient receptacle;
    private final Fluid fluid;
    private final int amount;
    private final NBTTagCompound attributes;

    private FillReceptacleRecipe(@Nullable ResourceLocation group, NonNullList<Ingredient> ingredients,
                                 Ingredient receptacle, Fluid fluid, int amount, NBTTagCompound attributes) {
        super(group, ingredients, ItemStack.EMPTY);
        this.receptacle = receptacle;
        this.fluid = fluid;
        this.amount = amount;
        this.attributes = attributes;
        // Forge's simple matcher skips named and enchanted stacks.
        isSimple = false;
        for (ItemStack stack : receptacle.getMatchingStacks()) {
            output = fill(stack);
            if (!output.isEmpty()) break;
        }
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return super.matches(inventory, world) && !getCraftingResult(inventory).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && receptacle.apply(stack)) {
                if (!result.isEmpty()) return ItemStack.EMPTY;
                result = fill(stack);
            }
        }
        return result;
    }

    private ItemStack fill(ItemStack stack) {
        FluidVesselHandler vessel = FluidVesselHandler.forCrafting(stack);
        return vessel == null ? ItemStack.EMPTY : vessel.craftContents(fluid, amount, attributes);
    }

    public List<ItemStack> getDisplayOutputs() {
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : receptacle.getMatchingStacks()) {
            ItemStack filled = fill(stack);
            if (!filled.isEmpty()) outputs.add(filled);
        }
        return outputs;
    }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            String group = JsonUtils.getString(json, "group", "");
            Ingredient receptacle = CraftingHelper.getIngredient(json.get("receptical"), context);
            NonNullList<Ingredient> ingredients = NonNullList.create();
            ingredients.add(receptacle);
            for (JsonElement ingredient : JsonUtils.getJsonArray(json, "ingredients")) {
                ingredients.add(CraftingHelper.getIngredient(ingredient, context));
            }
            if (ingredients.size() < 2 || ingredients.size() > 9) {
                throw new JsonSyntaxException("Expected one receptacle and one to eight ingredients");
            }

            JsonObject result = JsonUtils.getJsonObject(json, "result");
            Fluid fluid = FluidStackIngredient.readFluid(result);
            NBTTagCompound attributes = new NBTTagCompound();
            if (result.has("attributes")) {
                try {
                    attributes = JsonToNBT.getTagFromJson(result.get("attributes").toString());
                } catch (NBTException exception) {
                    throw new JsonSyntaxException("Invalid fluid attributes", exception);
                }
            }
            return new FillReceptacleRecipe(group.isEmpty() ? null : new ResourceLocation(group), ingredients,
                    receptacle, fluid, JsonUtils.getInt(result, "level", -1), attributes);
        }
    }
}
