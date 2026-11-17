/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.compat.jei;

import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.item.HarmoniumItem;
import ivorius.psychedelicraft.recipe.*;
import mezz.jei.api.gui.builder.*;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.StainedGlassBlock;
import java.util.*;

final class FluidCraftingExtension implements ICraftingCategoryExtension {
    private final CraftingRecipe recipe;
    FluidCraftingExtension(CraftingRecipe recipe) { this.recipe = recipe; }
    @Override public ResourceLocation getRegistryName() { return recipe.getId(); }
    @Override public int getWidth() { return recipe instanceof BottleRecipe bottle ? bottle.getWidth() : 0; }
    @Override public int getHeight() { return recipe instanceof BottleRecipe bottle ? bottle.getHeight() : 0; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        recipe.getIngredients().forEach(ingredient -> inputs.add(new ArrayList<>(Arrays.asList(ingredient.getItems()))));
        List<ItemStack> outputs = new ArrayList<>(List.of(recipe.getResultItem(RegistryAccess.EMPTY)));
        List<Integer> linked = new ArrayList<>();
        if (recipe instanceof FillReceptacleRecipe fill) {
            outputs.clear();
            List<ItemStack> vessels = new ArrayList<>();
            var results = fill.getDisplayOutputs();
            for (int i = 0; i < results.size(); i++) if (!results.get(i).isEmpty()) {
                vessels.add(inputs.get(0).get(i));
                outputs.add(results.get(i));
            }
            inputs.set(0, vessels);
            linked.add(0);
        } else if (recipe instanceof FluidAwareShapelessRecipe fluid) {
            for (int i = 0; i < inputs.size(); i++) {
                var ingredient = fluid.getFluidAwareIngredients().get(i);
                inputs.set(i, inputs.get(i).stream().filter(ingredient::test).toList());
            }
        } else if (recipe instanceof BottleRecipe) {
            // Keep every color aligned across all glass slots and the searchable output.
            for (int i = 0; i < inputs.size(); i++) {
                if (inputs.get(i).stream().noneMatch(FluidCraftingExtension::isGlass)) continue;
                if (linked.isEmpty()) {
                    outputs.clear();
                    for (ItemStack glass : inputs.get(i)) {
                        ItemStack output = recipe.getResultItem(RegistryAccess.EMPTY).copy();
                        if (glass.getItem() instanceof BlockItem item && item.getBlock() instanceof StainedGlassBlock stained) {
                            HarmoniumItem.setColor(output, stained.getColor().getTextColor());
                        }
                        outputs.add(output);
                    }
                }
                if (inputs.get(i).size() == outputs.size()) linked.add(i);
            }
        }
        var output = grid.createAndSetOutputs(builder, outputs);
        List<List<ItemStack>> padded = new ArrayList<>(Collections.nCopies(9, List.of()));
        int width = getWidth() == 0 ? 3 : getWidth();
        for (int i = 0; i < inputs.size(); i++) padded.set(i / width * 3 + i % width, inputs.get(i));
        var slots = grid.createAndSetInputs(builder, padded, 3, 3);
        if (getWidth() == 0) builder.setShapeless();
        List<IRecipeSlotBuilder> links = new ArrayList<>();
        links.add(output);
        for (int index : linked) links.add(slots.get(index / width * 3 + index % width));
        if (links.size() > 1) builder.createFocusLink(links);
        for (int i = 0; i < inputs.size(); i++) {
            final int index = i;
            slots.get(i / width * 3 + i % width).addTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(stack -> {
                ItemStack remainder = recipe instanceof FluidAwareShapelessRecipe fluid
                        ? fluid.getFluidAwareIngredients().get(index).remainder(stack)
                        : recipe instanceof FillReceptacleRecipe && index == 0 ? ItemStack.EMPTY : stack.getCraftingRemainingItem();
                if (remainder.isEmpty()) return;
                tooltip.add(Component.translatable("psychedelicraft.jei.crafting_returns", remainder.getHoverName()).withStyle(ChatFormatting.GRAY));
                var handler = FluidVesselHandler.forCrafting(remainder);
                if (handler != null && !handler.getFluid().isEmpty()) tooltip.add(Component.translatable("psychedelicraft.drink.levels",
                        handler.getFluid().getAmount(), handler.getTankCapacity(0)).withStyle(ChatFormatting.GRAY));
            }));
        }
    }
    private static boolean isGlass(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof net.minecraft.world.level.block.AbstractGlassBlock;
    }
}
