/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 7 Feb 2023.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;

public final class MashingRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public static final int MAX_INGREDIENTS = 8;

    private final Fluid pool;
    private final int minimumAmount;
    private final NBTTagCompound poolAttributes;
    private final Fluid result;
    private final NBTTagCompound resultAttributes;
    private final NonNullList<Ingredient> ingredients;
    private final int stewTime;

    private MashingRecipe(Fluid pool, int minimumAmount, NBTTagCompound poolAttributes, Fluid result,
                          NBTTagCompound resultAttributes, NonNullList<Ingredient> ingredients, int stewTime) {
        this.pool = pool;
        this.minimumAmount = minimumAmount;
        this.poolAttributes = poolAttributes;
        this.result = result;
        this.resultAttributes = resultAttributes;
        this.ingredients = ingredients;
        this.stewTime = stewTime;
    }

    public boolean matchesPool(@Nullable FluidStack contents) {
        return contents != null && contents.getFluid() == pool && contents.amount >= minimumAmount
                && (poolAttributes.hasNoTags() || NBTUtil.areNBTEquals(poolAttributes, contents.tag, true));
    }

    public boolean acceptsIngredient(ItemStack stack) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.apply(stack));
    }

    public FluidStack getMinimumPool() {
        return new FluidStack(pool, minimumAmount, poolAttributes.hasNoTags() ? null : poolAttributes.copy());
    }

    public boolean matchesPartial(List<ItemStack> supplied) {
        if (supplied.size() > ingredients.size()) return false;
        return assign(supplied, 0, 0);
    }

    public boolean matchesIngredients(List<ItemStack> supplied) {
        return supplied.size() == ingredients.size() && matchesPartial(supplied);
    }

    private boolean assign(List<ItemStack> supplied, int index, int occupied) {
        if (index == supplied.size()) return true;
        for (int i = 0; i < ingredients.size(); i++) {
            int bit = 1 << i;
            if ((occupied & bit) == 0 && ingredients.get(i).apply(supplied.get(index))
                    && assign(supplied, index + 1, occupied | bit)) return true;
        }
        return false;
    }

    public FluidStack finish(int amount) {
        return new FluidStack(result, amount, resultAttributes.hasNoTags() ? null : resultAttributes.copy());
    }

    public int getStewTime() {
        return stewTime;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    // Forge 1.12 loads machine recipes through the same JSON registry as table recipes.
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
        ItemStack vat = new ItemStack(PSItems.MASH_TUB);
        vat.setTagInfo("fluid", FluidContents.write(finish(PSItems.MASH_TUB.getCapacity())));
        return vat;
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
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (JsonElement ingredient : JsonUtils.getJsonArray(json, "ingredients")) {
                ingredients.add(CraftingHelper.getIngredient(ingredient, context));
            }
            if (ingredients.isEmpty() || ingredients.size() > MAX_INGREDIENTS) {
                throw new JsonSyntaxException("Expected one to eight mashing ingredients");
            }
            JsonObject pool = JsonUtils.getJsonObject(json, "base_fluid");
            JsonObject result = JsonUtils.getJsonObject(json, "result");
            int stewTime = Math.max(0, JsonUtils.getInt(json, "stew_time", 0));
            return new MashingRecipe(FluidStackIngredient.readFluid(pool), JsonUtils.getInt(pool, "level", 1),
                    attributes(pool), FluidStackIngredient.readFluid(result), attributes(result), ingredients, stewTime);
        }

        private static NBTTagCompound attributes(JsonObject json) {
            if (!json.has("attributes")) return new NBTTagCompound();
            try {
                return JsonToNBT.getTagFromJson(json.get("attributes").toString());
            } catch (NBTException exception) {
                throw new JsonSyntaxException("Invalid fluid attributes", exception);
            }
        }
    }
}
