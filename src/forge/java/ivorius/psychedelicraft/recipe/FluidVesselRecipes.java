package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class FluidVesselRecipes {
    private FluidVesselRecipes() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().registerAll(
                new BottleRecipe().setRegistryName(Psychedelicraft.id("bottle")),
                new CoffeeRecipe().setRegistryName(Psychedelicraft.id("cold_coffee")),
                new PouringRecipe().setRegistryName(Psychedelicraft.id("pouring")));
    }

    private static final class BottleRecipe extends ShapedOreRecipe {
        private BottleRecipe() {
            super(null, new ItemStack(PSItems.BOTTLE, 8), " # ", "# #", "###", '#', "blockGlass");
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack glass = inventory.getStackInSlot(slot);
                if (glass.getItem() == Item.getItemFromBlock(Blocks.STAINED_GLASS)) {
                    NBTTagCompound display = new NBTTagCompound();
                    display.setInteger("color", EnumDyeColor.byMetadata(glass.getMetadata()).getColorValue());
                    result.setTagInfo("display", display);
                    break;
                }
            }
            return result;
        }
    }

    private static final class CoffeeRecipe extends ShapelessOreRecipe {
        private CoffeeRecipe() {
            super(null, new ItemStack(PSItems.STONE_CUP), PSItems.STONE_CUP, Items.WATER_BUCKET,
                    PSItems.COFFEE_BEANS, PSItems.COFFEE_BEANS);
        }

        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            return super.matches(inventory, world) && !getCraftingResult(inventory).isEmpty();
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack cup = inventory.getStackInSlot(slot);
                if (cup.getItem() == PSItems.STONE_CUP && PSItems.STONE_CUP.contents(cup) == null) {
                    return brew(cup);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return brew(new ItemStack(PSItems.STONE_CUP));
        }

        private static ItemStack brew(ItemStack cup) {
            FluidVesselHandler handler = FluidVesselHandler.forCrafting(cup);
            handler.fill(new FluidStack(PSFluids.COFFEE, 500), true);
            return handler.getContainer();
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
            FluidVesselHandler source = null;
            FluidVesselHandler target = null;
            int sourceSlot = -1;
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack input = inventory.getStackInSlot(slot);
                if (input.isEmpty()) continue;
                FluidVesselHandler vessel = FluidVesselHandler.forCrafting(input);
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
