/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.item.FluidMachineItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;

public final class ChangeReceptacleRecipe extends ShapelessOreRecipe {
    private ChangeReceptacleRecipe(@Nullable ResourceLocation group, NonNullList<Ingredient> ingredients, ItemStack output) {
        super(group, ingredients, output);
        // Use the full matcher for vessels with custom names or other item data.
        isSimple = false;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return super.matches(inventory, world) && !getCraftingResult(inventory).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        int slot = receptacleSlot(inventory);
        if (slot < 0) return ItemStack.EMPTY;

        ItemStack source = inventory.getStackInSlot(slot);
        ItemStack target = output.copy();
        FluidStack contents = vessel(source).getTankProperties()[0].getContents();
        if (contents != null) {
            IFluidHandlerItem to = vessel(target);
            if (to == null || target.getCount() != 1) return ItemStack.EMPTY;
            IFluidTankProperties tank = to.getTankProperties()[0];
            FluidStack existing = tank.getContents();
            int level = existing == null ? 0 : existing.amount;
            if (contents.amount > tank.getCapacity() - level) return ItemStack.EMPTY;
            FluidStack combined = new FluidStack(contents, level + contents.amount);
            if (to instanceof FluidVesselHandler) {
                target = ((FluidVesselHandler) to).replaceContents(combined);
            } else {
                target.setTagInfo("fluid", FluidContents.write(combined));
            }
        }
        if (BottleRecipe.isDyeable(source) && BottleRecipe.isDyeable(target)) {
            BottleRecipe.setColor(target, BottleRecipe.getColor(source));
        }
        return target;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        int source = receptacleSlot(inventory);
        for (int slot = 0; slot < remaining.size(); slot++) {
            if (slot != source) remaining.set(slot, ForgeHooks.getContainerItem(inventory.getStackInSlot(slot)));
        }
        return remaining;
    }

    private static int receptacleSlot(InventoryCrafting inventory) {
        int source = -1;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (vessel(inventory.getStackInSlot(slot)) == null) continue;
            if (source >= 0) return -1;
            source = slot;
        }
        return source;
    }

    @Nullable
    private static IFluidHandlerItem vessel(ItemStack stack) {
        return stack.getItem() instanceof FluidMachineItem
                ? FluidVesselHandler.forTransfer(stack) : FluidVesselHandler.forCrafting(stack);
    }

    public static final class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            ShapelessOreRecipe recipe = ShapelessOreRecipe.factory(context, json);
            if (recipe.getIngredients().size() > 9 || recipe.getRecipeOutput().isEmpty()) {
                throw new JsonSyntaxException("Invalid receptacle conversion ingredients or output");
            }
            String group = recipe.getGroup();
            return new ChangeReceptacleRecipe(group.isEmpty() ? null : new ResourceLocation(group),
                    recipe.getIngredients(), recipe.getRecipeOutput());
        }
    }
}
