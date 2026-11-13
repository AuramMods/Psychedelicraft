package ivorius.psychedelicraft.compat.jei;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.BrewFluid;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

final class ProcessCategory implements IRecipeCategory<ProcessRecipe> {
    private final String name;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    ProcessCategory(IGuiHelper gui, String name, Item catalyst) {
        this.name = name;
        background = gui.createBlankDrawable(154, 77);
        icon = gui.createDrawableIngredient(new ItemStack(catalyst));
        slot = gui.getSlotDrawable();
        arrow = gui.createDrawable(new ResourceLocation("textures/gui/container/furnace.png"), 176, 14, 24, 17);
    }

    static String uid(String name) {
        return Psychedelicraft.id(name).toString();
    }

    @Override
    public String getUid() {
        return uid(name);
    }

    @Override
    public String getTitle() {
        return I18n.format("psychedelicraft.jei." + name);
    }

    @Override
    public String getModName() {
        return "Psychedelicraft";
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void drawExtras(Minecraft client) {
        arrow.draw(client, 77, 12);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, ProcessRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        for (int i = 0; i < recipe.itemsIn.size(); i++) {
            items.init(i, true, i % 3 * 18, i / 3 * 18);
            items.setBackground(i, slot);
            items.set(i, recipe.itemsIn.get(i));
        }
        for (int i = 0; i < recipe.itemsOut.size(); i++) {
            int index = recipe.itemsIn.size() + i;
            items.init(index, false, 134, i * 18);
            items.setBackground(index, slot);
            items.set(index, recipe.itemsOut.get(i));
        }
        IGuiFluidStackGroup fluids = layout.getFluidStacks();
        for (int i = 0; i < recipe.fluidsIn.size(); i++) {
            tank(fluids, i, true, 58, i * 18, recipe.fluidsIn.get(i));
        }
        for (int i = 0; i < recipe.fluidsOut.size(); i++) {
            tank(fluids, recipe.fluidsIn.size() + i, false, 102 + i % 2 * 18, i / 2 * 18, recipe.fluidsOut.get(i));
        }
        fluids.addTooltipCallback((index, input, fluid, tooltip) -> {
            if (fluid.getFluid() instanceof BrewFluid) BrewFluid.appendAttributes(fluid, tooltip);
        });
    }

    private void tank(IGuiFluidStackGroup fluids, int index, boolean input, int x, int y, FluidStack fluid) {
        fluids.init(index, input, x, y, 16, 16, fluid.amount, false, null);
        fluids.setBackground(index, slot);
        fluids.set(index, fluid);
    }
}
