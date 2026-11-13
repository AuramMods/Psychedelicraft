/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.FluidVessel;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Supplier;

import static ivorius.psychedelicraft.item.PSItems.*;

/** The pinned creative catalog, with the supplies added for the Forge backport. */
final class PSItemGroups {
    static final CreativeTabs GENERAL = new Group("general", () -> CANNABIS_LEAF, PSItemGroups::generalItems);
    static final CreativeTabs DRINKS = new Group("drinks", () -> OAK_BARREL, () -> new Item[]{
            STONE_CUP, SHOT_GLASS, WOODEN_MUG, GLASS_CHALICE, BOTTLE, FILLED_BUCKET, FILLED_BOWL, FILLED_GLASS_BOTTLE
    });
    static final CreativeTabs WEAPONS = new Group("weapons", () -> MOLOTOV_COCKTAIL, () -> new Item[]{MOLOTOV_COCKTAIL});

    private PSItemGroups() {}

    private static Item[] generalItems() {
        return new Item[]{
                DRYING_TABLE, IRON_DRYING_TABLE, FLASK, DISTILLERY, BOTTLE_RACK, MASH_TUB,
                OAK_BARREL, BIRCH_BARREL, SPRUCE_BARREL, ACACIA_BARREL, JUNGLE_BARREL, DARK_OAK_BARREL,
                RIFT_JAR, TRAY, BUNSEN_BURNER, SMOKING_PIPE, CIGARETTE, CIGAR, JOINT, BONG, SYRINGE,
                COFFEA_CHERRIES, COFFEE_BEANS, TOMATO_LEAF, TOMATO_SEEDS, TOMATO, OBSIDIAN_BOTTLE, OBSIDIAN_DUST,
                MORNING_GLORY, MORNING_GLORY_SEEDS, LSA_SQUARE, LSD_PILL,
                JIMSONWEED_SEEDS, JIMSONWEED_SEED_POD, JIMSONWEED_LEAF, DRIED_JIMSONWEED_LEAF,
                BELLADONNA_SEEDS, BELLADONNA_BERRIES, BELLADONNA_LEAF, DRIED_BELLADONNA_LEAF,
                TOBACCO_SEEDS, TOBACCO_LEAVES, DRIED_TOBACCO, COCA_SEEDS, COCA_LEAVES, DRIED_COCA_LEAVES, COCAINE_POWDER,
                AGAVE_LEAF, PEYOTE, DRIED_PEYOTE, PEYOTE_JOINT, HOP_SEEDS, HOP_CONES,
                CANNABIS_SEEDS, CANNABIS_LEAF, DRIED_CANNABIS_LEAF, CANNABIS_BUDS, DRIED_CANNABIS_BUDS, HASH_MUFFIN,
                BROWN_MAGIC_MUSHROOMS, RED_MAGIC_MUSHROOMS, LATTICE, WINE_GRAPES,
                JUNIPER_LEAVES, FRUITING_JUNIPER_LEAVES, JUNIPER_LOG, JUNIPER_WOOD, STRIPPED_JUNIPER_LOG,
                STRIPPED_JUNIPER_WOOD, JUNIPER_SAPLING, JUNIPER_BERRIES, JUNIPER_PLANKS, JUNIPER_STAIRS, JUNIPER_SIGN,
                JUNIPER_DOOR, JUNIPER_HANGING_SIGN, JUNIPER_PRESSURE_PLATE, JUNIPER_FENCE, JUNIPER_TRAPDOOR,
                JUNIPER_FENCE_GATE, JUNIPER_BUTTON, JUNIPER_SLAB, JUNIPER_BOAT, JUNIPER_CHEST_BOAT, PAPER_BAG,
                BEE_NEST, HONEYCOMB, HONEY_BOTTLE, COPPER_ORE, RAW_COPPER, COPPER_INGOT,
                IRON_CHAIN, GLOWING_INK, SIGN_WAX, SIGN_BLACK_DYE,
                HARMONIUM
        };
    }

    @SideOnly(Side.CLIENT)
    static void addVesselStacks(PortableFluidItem item, CreativeTabs tab, NonNullList<ItemStack> stacks) {
        FluidVessel vessel = item.getVessel();
        Item empty = vessel.emptyItem();
        if (empty == null || tab != CreativeTabs.SEARCH) stacks.add(new ItemStack(empty == null ? item : empty));

        if (vessel == FluidVessel.SYRINGE) {
            addFilled(stacks, item, new FluidStack(PSFluids.COCAINE, vessel.capacity));
            addFilled(stacks, item, new FluidStack(PSFluids.CAFFEINE, vessel.capacity));
            addFilled(stacks, item, new FluidStack(PSFluids.BATH_SALTS, vessel.capacity));
            NBTTagCompound extract = new NBTTagCompound();
            extract.setInteger(BrewFluid.DISTILLATION, 2);
            addFilled(stacks, item, new FluidStack(PSFluids.MORNING_GLORY_EXTRACT, vessel.capacity, extract));
        } else if (vessel == FluidVessel.SHOT_GLASS) {
            PSFluids.AGAVE.addCreativeVariants(vessel.capacity, true, fluid -> addFilled(stacks, item, fluid));
        } else {
            for (Fluid fluid : PSFluids.all()) {
                if (!isSuitable(fluid, vessel)) continue;
                if (fluid instanceof BrewFluid) {
                    ((BrewFluid) fluid).addCreativeVariants(vessel.capacity, false, contents -> addFilled(stacks, item, contents));
                } else if (fluid == PSFluids.COFFEE) {
                    addFilled(stacks, item, new FluidStack(fluid, vessel.capacity));
                    for (int warmth = 1; warmth <= 2; warmth++) {
                        NBTTagCompound attributes = new NBTTagCompound();
                        attributes.setInteger("warmth", warmth);
                        addFilled(stacks, item, new FluidStack(fluid, vessel.capacity, attributes));
                    }
                } else {
                    addFilled(stacks, item, new FluidStack(fluid, vessel.capacity));
                }
            }
        }
    }

    private static boolean isSuitable(Fluid fluid, FluidVessel vessel) {
        if (fluid == PSFluids.COFFEE) return vessel == FluidVessel.CUP;
        if (fluid instanceof BrewFluid) return vessel != FluidVessel.CUP;
        return fluid == PSFluids.SLURRY || (vessel != FluidVessel.MUG && !PSFluids.isInjectable(fluid));
    }

    private static void addFilled(NonNullList<ItemStack> stacks, PortableFluidItem item, FluidStack contents) {
        ItemStack stack = new ItemStack(item);
        // Creative samples keep the donor's filled item, including the milk and honey worts.
        stack.setTagInfo("fluid", FluidContents.write(contents));
        stacks.add(stack);
    }

    private static final class Group extends CreativeTabs {
        private final Supplier<Item> icon;
        private final Supplier<Item[]> entries;

        Group(String name, Supplier<Item> icon, Supplier<Item[]> entries) {
            super("psychedelicraft." + name);
            this.icon = icon;
            this.entries = entries;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getTabIconItem() {
            return new ItemStack(icon.get());
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void displayAllRelevantItems(NonNullList<ItemStack> stacks) {
            for (Item item : entries.get()) item.getSubItems(this, stacks);
        }
    }
}
