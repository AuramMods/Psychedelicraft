package ivorius.psychedelicraft.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

final class ProcessRecipe implements IRecipeWrapper {
    final List<List<ItemStack>> itemsIn;
    final List<ItemStack> itemsOut;
    final List<FluidStack> fluidsIn;
    final List<FluidStack> fluidsOut;
    private final Supplier<String> detail;

    ProcessRecipe(List<List<ItemStack>> itemsIn, List<ItemStack> itemsOut, List<FluidStack> fluidsIn,
                  List<FluidStack> fluidsOut, Supplier<String> detail) {
        this.itemsIn = itemsIn;
        this.itemsOut = itemsOut;
        this.fluidsIn = fluidsIn;
        this.fluidsOut = fluidsOut;
        this.detail = detail;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, itemsIn);
        ingredients.setOutputs(VanillaTypes.ITEM, itemsOut);
        ingredients.setInputs(VanillaTypes.FLUID, fluidsIn);
        ingredients.setOutputs(VanillaTypes.FLUID, fluidsOut);
    }

    @Override
    public void drawInfo(Minecraft client, int width, int height, int mouseX, int mouseY) {
        client.fontRenderer.drawSplitString(detail.get(), 0, 56, width, 0x404040);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return mouseY >= 55 ? Collections.singletonList(detail.get()) : Collections.emptyList();
    }
}
