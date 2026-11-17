/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.compat.jei;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.BrewProfile;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class ProcessCategory implements IRecipeCategory<ProcessRecipe> {
    static final int WIDTH = 154, HEIGHT = 94;
    private final RecipeType<ProcessRecipe> type;
    private final IDrawable icon, slot, background;

    ProcessCategory(IGuiHelper gui, RecipeType<ProcessRecipe> type, Item catalyst) {
        this.type = type;
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(catalyst));
        slot = gui.getSlotDrawable();
        background = gui.createBlankDrawable(WIDTH, HEIGHT);
    }
    static RecipeType<ProcessRecipe> type(String name) {
        return new RecipeType<>(Psychedelicraft.id(name), ProcessRecipe.class);
    }
    @Override public RecipeType<ProcessRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable("psychedelicraft.jei." + type.getUid().getPath()); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public IDrawable getBackground() { return background; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ProcessRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.itemsIn().size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i % 3 * 18, 1 + i / 3 * 18)
                    .setBackground(slot, -1, -1).addItemStacks(recipe.itemsIn().get(i));
        }
        for (int i = 0; i < recipe.itemsOut().size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 135, 1 + i * 18)
                    .setBackground(slot, -1, -1).addItemStack(recipe.itemsOut().get(i));
        }
        for (int i = 0; i < recipe.fluidsIn().size(); i++) {
            fluid(builder, RecipeIngredientRole.INPUT, 59, 1 + i * 18, recipe.fluidsIn().get(i));
        }
        for (int i = 0; i < recipe.fluidsOut().size(); i++) {
            fluid(builder, RecipeIngredientRole.OUTPUT, 103 + i % 2 * 18, 1 + i / 2 * 18, recipe.fluidsOut().get(i));
        }
    }
    private void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
                       net.minecraftforge.fluids.FluidStack fluid) {
        builder.addSlot(role, x, y).setBackground(slot, -1, -1)
                .setFluidRenderer(fluid.getAmount(), false, 16, 16).addIngredient(ForgeTypes.FLUID_STACK, fluid)
                .addTooltipCallback((view, tooltip) -> view.getDisplayedIngredient(ForgeTypes.FLUID_STACK)
                        .ifPresent(stack -> BrewProfile.appendAttributes(stack, tooltip)));
    }
    @Override public void draw(ProcessRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "→", 81, 6, 0xff444444, false);
        var lines = font.split(recipe.detail(), WIDTH);
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), 0, 58 + i * 10, 0xff555555, false);
    }
}
