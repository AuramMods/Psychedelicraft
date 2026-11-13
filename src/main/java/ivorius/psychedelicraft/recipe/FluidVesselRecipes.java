package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.item.FluidMachineItem;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class FluidVesselRecipes {
    private FluidVesselRecipes() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().registerAll(
                new DistilleryRecipe().setRegistryName(Psychedelicraft.id("distillery")),
                new PouringRecipe().setRegistryName(Psychedelicraft.id("pouring")));
    }

    private static final class DistilleryRecipe extends ShapedOreRecipe {
        private DistilleryRecipe() {
            super(null, new ItemStack(PSItems.DISTILLERY), "##", "D ", '#', "ingotCopper", 'D', PSItems.FLASK);
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack input = inventory.getStackInSlot(slot);
                if (input.getItem() == PSItems.FLASK && input.getSubCompound("fluid") != null) {
                    result.setTagInfo("fluid", input.getSubCompound("fluid").copy());
                    break;
                }
            }
            return result;
        }
    }

    private static final class PouringRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            return pour(inventory) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            Pour result = pour(inventory);
            return result == null ? ItemStack.EMPTY : result.target;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
            NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
            Pour result = pour(inventory);
            if (result != null) remaining.set(result.sourceSlot, result.source);
            return remaining;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canFit(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public boolean isDynamic() {
            return true;
        }

        @Nullable
        private static Pour pour(InventoryCrafting inventory) {
            IFluidHandlerItem source = null;
            IFluidHandlerItem target = null;
            int sourceSlot = -1;
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack input = inventory.getStackInSlot(slot);
                if (input.isEmpty()) continue;
                IFluidHandlerItem vessel = input.getItem() instanceof FluidMachineItem
                        ? FluidVesselHandler.forTransfer(input) : FluidVesselHandler.forCrafting(input);
                if (vessel == null || target != null) return null;
                if (source == null) {
                    source = vessel;
                    sourceSlot = slot;
                } else {
                    target = vessel;
                }
            }
            if (target == null || FluidUtil.tryFluidTransfer(target, source, Integer.MAX_VALUE, true) == null) return null;
            return new Pour(sourceSlot, source.getContainer(), target.getContainer());
        }
    }

    private static final class Pour {
        private final int sourceSlot;
        private final ItemStack source;
        private final ItemStack target;

        private Pour(int sourceSlot, ItemStack source, ItemStack target) {
            this.sourceSlot = sourceSlot;
            this.source = source;
            this.target = target;
        }
    }
}
