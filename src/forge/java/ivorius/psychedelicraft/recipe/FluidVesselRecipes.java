package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.item.FluidMachineItem;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class FluidVesselRecipes {
    private FluidVesselRecipes() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().registerAll(
                new BottleRecipe(new ItemStack(PSItems.BOTTLE, 8), " # ", "# #", "###", '#', "blockGlass")
                        .setRegistryName(Psychedelicraft.id("bottle")),
                new BottleRecipe(new ItemStack(PSItems.MOLOTOV_COCKTAIL, 4), "P#", 'P', Items.PAPER, '#', "blockGlass")
                        .setRegistryName(Psychedelicraft.id("molotov_cocktail")),
                new BottleConversion(PSItems.BOTTLE, PSItems.MOLOTOV_COCKTAIL,
                        PSItems.BOTTLE, new ItemStack(Blocks.WOOL, 1, OreDictionary.WILDCARD_VALUE))
                        .setRegistryName(Psychedelicraft.id("bottle_to_molotov")),
                new BottleConversion(PSItems.MOLOTOV_COCKTAIL, PSItems.BOTTLE, PSItems.MOLOTOV_COCKTAIL)
                        .setRegistryName(Psychedelicraft.id("molotov_to_bottle")),
                new DistilleryRecipe().setRegistryName(Psychedelicraft.id("distillery")),
                new LsaSquareRecipe().setRegistryName(Psychedelicraft.id("lsa_square")),
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

    public static final class BottleRecipe extends ShapedOreRecipe {
        private BottleRecipe(ItemStack output, Object... pattern) {
            super(null, output, pattern);
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack glass = inventory.getStackInSlot(slot);
                if (glass.getItem() == Item.getItemFromBlock(Blocks.STAINED_GLASS)) {
                    dye(result, EnumDyeColor.byMetadata(glass.getMetadata()));
                    break;
                }
            }
            return result;
        }

        public ItemStack coloredOutput(EnumDyeColor color) {
            ItemStack result = getRecipeOutput().copy();
            dye(result, color);
            return result;
        }

        private static void dye(ItemStack stack, EnumDyeColor color) {
            NBTTagCompound display = new NBTTagCompound();
            display.setInteger("color", color.getColorValue());
            stack.setTagInfo("display", display);
        }
    }

    private static final class BottleConversion extends ShapelessOreRecipe {
        private final PortableFluidItem source;
        private final PortableFluidItem target;

        private BottleConversion(PortableFluidItem source, PortableFluidItem target, Object... ingredients) {
            super(null, new ItemStack(target), ingredients);
            this.source = source;
            this.target = target;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack input = inventory.getStackInSlot(slot);
                if (input.getItem() != source) continue;
                FluidStack contents = source.contents(input);
                if (contents != null) {
                    result = new FluidVesselHandler(result, target.getVessel())
                            .craftContents(contents.getFluid(), contents.amount, contents.tag);
                }
                NBTTagCompound display = input.getSubCompound("display");
                if (display != null && display.hasKey("color", 99)) {
                    NBTTagCompound dye = new NBTTagCompound();
                    dye.setInteger("color", display.getInteger("color"));
                    result.setTagInfo("display", dye);
                }
                break;
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
