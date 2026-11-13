/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Based on the fluid smelting recipe by Sollace.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.FluidMachineItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.JsonUtils;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntBinaryOperator;

public final class FurnaceRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    private static final String LAST_OUTPUT = "psychedelicraftSmeltedOutput";
    private static List<FurnaceRecipe> recipes = Collections.emptyList();

    @Nullable
    private final Ingredient ingredient;
    @Nullable
    private final Fluid fluid;
    private final int minimum;
    private final NBTTagCompound requiredAttributes;
    private final ItemStack result;
    private final Map<String, Change> changes;
    private final int duration;
    private final float experience;

    private FurnaceRecipe(@Nullable Ingredient ingredient, @Nullable Fluid fluid, int minimum,
                          NBTTagCompound requiredAttributes, ItemStack result, Map<String, Change> changes,
                          int duration, float experience) {
        this.ingredient = ingredient;
        this.fluid = fluid;
        this.minimum = minimum;
        this.requiredAttributes = requiredAttributes;
        this.result = result;
        this.changes = changes;
        this.duration = duration;
        this.experience = experience;
    }

    public static void load() {
        List<FurnaceRecipe> loaded = new ArrayList<>();
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            if (recipe instanceof FurnaceRecipe) loaded.add((FurnaceRecipe) recipe);
        }
        loaded.sort(Comparator.comparing((FurnaceRecipe recipe) -> recipe.ingredient != null).reversed()
                .thenComparing(recipe -> recipe.requiredAttributes.hasNoTags())
                .thenComparing(Comparator.comparingInt((FurnaceRecipe recipe) -> recipe.minimum).reversed()));
        recipes = Collections.unmodifiableList(loaded);
    }

    public static List<FurnaceRecipe> recipes() {
        return recipes;
    }

    @Nullable
    public FluidStack exampleFluid() {
        return fluid == null ? null : new FluidStack(fluid, minimum, requiredAttributes.copy());
    }

    public ItemStack[] exampleItems() {
        return ingredient == null ? new ItemStack[0] : ingredient.getMatchingStacks();
    }

    public float getExperience() {
        return experience;
    }

    @Nullable
    public static FurnaceRecipe find(ItemStack input) {
        if (!input.isEmpty()) {
            for (FurnaceRecipe recipe : recipes) {
                if (!recipe.resultFor(input).isEmpty()) return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack lookup(ItemStack input) {
        if (!input.isEmpty()) {
            for (FurnaceRecipe recipe : recipes) {
                ItemStack output = recipe.resultFor(input);
                if (!output.isEmpty()) return output;
            }
        }
        return null;
    }

    public static int cookTime(int vanilla, ItemStack input) {
        FurnaceRecipe recipe = find(input);
        return recipe == null ? vanilla : recipe.duration;
    }

    public static void restoreDuration(TileEntityFurnace furnace) {
        FurnaceRecipe recipe = find(furnace.getStackInSlot(0));
        if (recipe != null && furnace.getField(3) != recipe.duration) {
            furnace.setField(3, recipe.duration);
            furnace.setField(2, Math.min(furnace.getField(2), recipe.duration - 1));
        }
    }

    public static boolean canMerge(ItemStack output, ItemStack result, TileEntityFurnace furnace) {
        if (!output.isItemEqual(result)) return false;
        FurnaceRecipe recipe = find(furnace.getStackInSlot(0));
        NBTTagCompound previous = storedOutput(furnace, output);
        if (recipe == null && previous == null) return true;
        if (!ItemStack.areItemStackTagsEqual(output, result)) return false;
        float existingExperience = previous == null ? FurnaceRecipes.instance().getSmeltingExperience(output)
                : previous.getFloat("Experience");
        float nextExperience = recipe == null ? FurnaceRecipes.instance().getSmeltingExperience(result) : recipe.experience;
        return existingExperience == nextExperience;
    }

    public static void recordOutput(TileEntityFurnace furnace) {
        FurnaceRecipe recipe = find(furnace.getStackInSlot(0));
        if (recipe == null) {
            furnace.getTileData().removeTag(LAST_OUTPUT);
            return;
        }
        ItemStack output = furnace.getStackInSlot(2).copy();
        output.setCount(1);
        NBTTagCompound record = output.writeToNBT(new NBTTagCompound());
        record.setFloat("Experience", recipe.experience);
        furnace.getTileData().setTag(LAST_OUTPUT, record);
    }

    public static float takenExperience(FurnaceRecipes registry, ItemStack output, IInventory inventory) {
        if (inventory instanceof TileEntityFurnace) {
            NBTTagCompound record = storedOutput((TileEntityFurnace) inventory, output);
            if (record != null) return record.getFloat("Experience");
        }
        return registry.getSmeltingExperience(output);
    }

    public static float recipeExperience(float vanilla, ItemStack output) {
        if (vanilla == 0 && !output.isEmpty()) {
            for (FurnaceRecipe recipe : recipes) {
                if (recipe.fluid == null && output.isItemEqual(recipe.result)
                        && ItemStack.areItemStackTagsEqual(output, recipe.result)) return recipe.experience;
            }
            FluidStack contents = contents(output);
            if (contents != null && contents.getFluid() == PSFluids.COFFEE && PSFluids.warmth(contents) > 0) {
                IRecipe recipe = ForgeRegistries.RECIPES.getValue(Psychedelicraft.id("hot_coffee"));
                if (recipe instanceof FurnaceRecipe) return ((FurnaceRecipe) recipe).experience;
            }
        }
        return vanilla;
    }

    @Nullable
    private static NBTTagCompound storedOutput(TileEntityFurnace furnace, ItemStack output) {
        NBTTagCompound record = furnace.getTileData().getCompoundTag(LAST_OUTPUT);
        if (record.hasNoTags()) return null;
        ItemStack previous = new ItemStack(record);
        return output.isItemEqual(previous) && ItemStack.areItemStackTagsEqual(output, previous) ? record : null;
    }

    public ItemStack resultFor(ItemStack input) {
        if (ingredient != null && !ingredient.apply(input)) return ItemStack.EMPTY;
        if (fluid == null) return result.copy();

        FluidStack source = contents(input);
        if (source == null || source.getFluid() != fluid || source.amount < minimum
                || (!requiredAttributes.hasNoTags() && !NBTUtil.areNBTEquals(requiredAttributes, source.tag, true))) {
            return ItemStack.EMPTY;
        }

        ItemStack output = result.isEmpty() ? input.copy() : result.copy();
        if (result.isEmpty()) output.setCount(1);
        FluidStack cooked = source.copy();
        NBTTagCompound attributes = cooked.tag == null ? new NBTTagCompound() : cooked.tag;
        for (Map.Entry<String, Change> entry : changes.entrySet()) {
            String key = entry.getKey();
            Change change = entry.getValue();
            int value = change.op.applyAsInt(attributes.getInteger(key), change.value);
            if (fluid == PSFluids.COFFEE && "warmth".equals(key)) {
                if (value <= 0) attributes.removeTag(key);
                else attributes.setInteger(key, Math.min(2, value));
            } else {
                attributes.setInteger(key, value);
            }
        }
        cooked.tag = attributes.hasNoTags() ? null : attributes;
        if (result.isEmpty() && cooked.isFluidEqual(source)) return ItemStack.EMPTY;
        return withContents(output, cooked);
    }

    @Nullable
    private static FluidStack contents(ItemStack input) {
        if (input.getItem() instanceof FluidMachineItem) return ((FluidMachineItem) input.getItem()).contents(input);
        FluidVesselHandler vessel = FluidVesselHandler.forCrafting(input);
        return vessel == null ? null : vessel.getFluid();
    }

    public static ItemStack withContents(ItemStack output, FluidStack fluid) {
        if (output.getItem() instanceof FluidMachineItem) {
            int capacity = ((FluidMachineItem) output.getItem()).getCapacity();
            output.setTagInfo("fluid", FluidContents.write(new FluidStack(fluid, Math.min(capacity, fluid.amount))));
            return output;
        }
        FluidVesselHandler vessel = FluidVesselHandler.forCrafting(output);
        return vessel == null ? output : vessel.replaceContents(fluid);
    }

    @Override public boolean matches(InventoryCrafting inventory, World world) { return false; }
    @Override public ItemStack getCraftingResult(InventoryCrafting inventory) { return ItemStack.EMPTY; }
    @Override public ItemStack getRecipeOutput() { return result.copy(); }
    @Override public boolean canFit(int width, int height) { return false; }
    @Override public boolean isDynamic() { return true; }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            JsonObject input = JsonUtils.getJsonObject(json, "input", new JsonObject());
            Fluid fluid = input.has("fluid") ? FluidStackIngredient.readFluid(input) : null;
            Ingredient ingredient = json.has("item") ? CraftingHelper.getIngredient(json.get("item"), context) : null;
            JsonObject result = JsonUtils.getJsonObject(json, "result");
            ItemStack stack = result.has("item") ? CraftingHelper.getItemStack(result, context) : ItemStack.EMPTY;
            int duration = JsonUtils.getInt(json, "cookingtime", JsonUtils.getInt(json, "cookingTime", 200));
            float experience = JsonUtils.getFloat(json, "experience", 0);
            if ((fluid == null && (ingredient == null || stack.isEmpty())) || duration <= 0 || duration > Short.MAX_VALUE
                    || !Float.isFinite(experience) || experience < 0 || (fluid != null && stack.getCount() > 1)
                    || (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize())) {
                throw new JsonSyntaxException("Invalid furnace input, output, experience or cooking time");
            }
            NBTTagCompound required = new NBTTagCompound();
            if (input.has("attributes")) {
                JsonElement tag = input.get("attributes");
                try {
                    required = JsonToNBT.getTagFromJson(tag.isJsonPrimitive() ? tag.getAsString() : tag.toString());
                } catch (NBTException error) {
                    throw new JsonSyntaxException("Invalid furnace fluid attributes", error);
                }
            }
            Map<String, Change> changes = new LinkedHashMap<>();
            JsonUtils.getJsonObject(result, "attributes", new JsonObject()).entrySet().forEach(entry -> {
                JsonObject change = JsonUtils.getJsonObject(entry.getValue(), entry.getKey());
                String key = fluid == PSFluids.COFFEE && "temperature".equals(entry.getKey()) ? "warmth" : entry.getKey();
                changes.put(key, Change.parse(change));
            });
            if (fluid == null && !changes.isEmpty()) throw new JsonSyntaxException("Fluid modifications require a fluid input");
            return new FurnaceRecipe(ingredient, fluid, Math.max(1, JsonUtils.getInt(input, "level", 1)),
                    required, stack, changes, duration, experience);
        }
    }

    private static final class Change {
        final IntBinaryOperator op;
        final int value;

        Change(IntBinaryOperator op, int value) {
            this.op = op;
            this.value = value;
        }

        static Change parse(JsonObject json) {
            int value = JsonUtils.getInt(json, "value");
            switch (JsonUtils.getString(json, "type", "add").toLowerCase(Locale.ROOT)) {
                case "set": return new Change((a, b) -> b, value);
                case "subtract": return new Change((a, b) -> a - b, value);
                case "multiply": return new Change((a, b) -> a * b, value);
                case "divide":
                    if (value == 0) throw new JsonSyntaxException("Cannot divide a furnace fluid attribute by zero");
                    return new Change((a, b) -> a / b, value);
                case "add": return new Change((a, b) -> a + b, value);
                default: throw new JsonSyntaxException("Unknown furnace fluid attribute operation");
            }
        }
    }
}
