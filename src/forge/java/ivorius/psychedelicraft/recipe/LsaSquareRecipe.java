package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidVessel;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.IForgeRegistryEntry;

public final class LsaSquareRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return sourceSlot(inventory) >= 0;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return sourceSlot(inventory) >= 0 ? getRecipeOutput() : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        int source = sourceSlot(inventory);
        if (source >= 0) {
            FluidVesselHandler vessel = FluidVesselHandler.forCrafting(inventory.getStackInSlot(source));
            vessel.drain(1, true);
            remaining.set(source, vessel.getContainer());
        }
        return remaining;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(PSItems.LSA_SQUARE);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private static int sourceSlot(InventoryCrafting inventory) {
        boolean paper = false;
        int source = -1;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (!paper && stack.getItem() == Items.PAPER) {
                paper = true;
            } else if (source < 0 && stack.getItem() instanceof PortableFluidItem) {
                PortableFluidItem item = (PortableFluidItem) stack.getItem();
                if (item.getVessel() == FluidVessel.SYRINGE) return -1;
                FluidStack fluid = item.contents(stack);
                if (fluid == null || fluid.getFluid() != PSFluids.MORNING_GLORY_EXTRACT || fluid.tag == null
                        || !fluid.tag.hasKey(BrewFluid.DISTILLATION, Constants.NBT.TAG_INT)
                        || fluid.tag.getInteger(BrewFluid.DISTILLATION) != 2) return -1;
                source = slot;
            } else {
                return -1;
            }
        }
        return paper ? source : -1;
    }
}
