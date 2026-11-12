/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSItems {
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final CreativeTabs GENERAL = new CreativeTabs("psychedelicraft.general") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(COFFEE_BEANS);
        }
    };

    public static final Item COFFEE_BEANS = named("coffee_beans", new Item());
    public static final Item BROWN_MAGIC_MUSHROOMS = edible("brown_magic_mushrooms", 0, 0.1F, DrugType.BROWN_SHROOMS);
    public static final Item RED_MAGIC_MUSHROOMS = edible("red_magic_mushrooms", 0, 0.1F, DrugType.RED_SHROOMS);
    public static final Item CANNABIS_SEEDS = named("cannabis_seeds", new CropSeedItem(PSBlocks.CANNABIS, "cannabis_seeds"));
    public static final Item CANNABIS_LEAF = named("cannabis_leaf", new Item());
    public static final Item CANNABIS_BUDS = named("cannabis_buds", new Item());
    public static final Item DRIED_CANNABIS_LEAF = named("dried_cannabis_leaf", new Item());
    public static final Item DRIED_CANNABIS_BUDS = named("dried_cannabis_buds", new Item());
    public static final Item HOP_SEEDS = named("hop_seeds", new CropSeedItem(PSBlocks.HOP, "hop_seeds"));
    public static final Item HOP_CONES = named("hop_cones", new Item());
    public static final Item COCA_SEEDS = named("coca_seeds", new CropSeedItem(PSBlocks.COCA, "coca_seeds"));
    public static final Item COCA_LEAVES = named("coca_leaves", new Item());
    public static final Item DRIED_COCA_LEAVES = named("dried_coca_leaves", new Item());
    public static final Item TOBACCO_SEEDS = named("tobacco_seeds", new CropSeedItem(PSBlocks.TOBACCO, "tobacco_seeds"));
    public static final Item TOBACCO_LEAVES = named("tobacco", new Item());
    public static final Item DRIED_TOBACCO = named("dried_tobacco", new Item());
    public static final Item COFFEA_CHERRIES = named("coffea_cherries", new CropSeedItem(PSBlocks.COFFEA, "coffea_cherries"));
    public static final Item PEYOTE = named("peyote", new ItemBlock(PSBlocks.PEYOTE));
    public static final Item DRIED_PEYOTE = edible("dried_peyote", 0, 0.1F, DrugType.PEYOTE);
    public static final Item AGAVE_LEAF = named("agave_leaf", new CropSeedItem(PSBlocks.AGAVE_PLANT, "agave_leaf"));
    public static final Item JIMSONWEED_SEEDS = named("jimsonweed_seeds", new CropSeedItem(PSBlocks.JIMSONWEED, "jimsonweed_seeds"));
    public static final Item JIMSONWEED_SEED_POD = named("jimsonweed_seed_pod", new Item());
    public static final Item JIMSONWEED_LEAF = named("jimsonweed_leaf", new Item());
    public static final Item DRIED_JIMSONWEED_LEAF = named("dried_jimsonweed_leaf", new Item());
    public static final Item BELLADONNA_SEEDS = named("belladonna_seeds", new CropSeedItem(PSBlocks.BELLADONNA, "belladonna_seeds"));
    public static final Item BELLADONNA_BERRIES = edible("belladonna_berries", 1, 1.5F, DrugType.ATROPINE);
    public static final Item BELLADONNA_LEAF = named("belladonna_leaf", new Item());
    public static final Item DRIED_BELLADONNA_LEAF = named("dried_belladonna_leaf", new Item());
    public static final Item TOMATO_SEEDS = named("tomato_seeds", new CropSeedItem(PSBlocks.TOMATOES, "tomato_seeds"));
    public static final Item TOMATO = named("tomato", new EdibleItem(1, 1.9F));
    public static final Item TOMATO_LEAF = named("tomato_leaf", new Item());
    public static final Item LATTICE = named("lattice", new ItemBlock(PSBlocks.LATTICE));
    public static final Item MORNING_GLORY = named("morning_glory", new Item());
    public static final Item MORNING_GLORY_SEEDS = named("morning_glory_seeds",
            new CropSeedItem(PSBlocks.MORNING_GLORY, "morning_glory_seeds"));
    public static final Item MORNING_GLORY_LATTICE = named("morning_glory_lattice",
            new LatticeCropItem(PSBlocks.MORNING_GLORY_LATTICE)).setCreativeTab(null);
    public static final Item WINE_GRAPE_LATTICE = named("wine_grape_lattice",
            new LatticeCropItem(PSBlocks.WINE_GRAPE_LATTICE)).setCreativeTab(null);
    public static final Item WINE_GRAPES = named("wine_grapes", new WineGrapesItem());
    public static final Item JUNIPER_LOG = named("juniper_log", new ItemBlock(PSBlocks.JUNIPER_LOG));
    public static final Item JUNIPER_WOOD = named("juniper_wood", new ItemBlock(PSBlocks.JUNIPER_WOOD));
    public static final Item STRIPPED_JUNIPER_LOG = named("stripped_juniper_log", new ItemBlock(PSBlocks.STRIPPED_JUNIPER_LOG));
    public static final Item STRIPPED_JUNIPER_WOOD = named("stripped_juniper_wood", new ItemBlock(PSBlocks.STRIPPED_JUNIPER_WOOD));
    public static final Item JUNIPER_LEAVES = named("juniper_leaves", new ItemBlock(PSBlocks.JUNIPER_LEAVES));
    public static final Item FRUITING_JUNIPER_LEAVES = named("fruiting_juniper_leaves", new ItemBlock(PSBlocks.FRUITING_JUNIPER_LEAVES));
    public static final Item JUNIPER_SAPLING = named("juniper_sapling", new ItemBlock(PSBlocks.JUNIPER_SAPLING));
    public static final Item JUNIPER_PLANKS = named("juniper_planks", new ItemBlock(PSBlocks.JUNIPER_PLANKS));
    public static final Item JUNIPER_STAIRS = named("juniper_stairs", new ItemBlock(PSBlocks.JUNIPER_STAIRS));
    public static final Item JUNIPER_SLAB = named("juniper_slab",
            new ItemSlab(PSBlocks.JUNIPER_SLAB, PSBlocks.JUNIPER_SLAB, PSBlocks.JUNIPER_DOUBLE_SLAB).setHasSubtypes(false));
    public static final Item JUNIPER_FENCE = named("juniper_fence", new ItemBlock(PSBlocks.JUNIPER_FENCE));
    public static final Item JUNIPER_FENCE_GATE = named("juniper_fence_gate", new ItemBlock(PSBlocks.JUNIPER_FENCE_GATE));
    public static final Item JUNIPER_DOOR = named("juniper_door", new ItemDoor(PSBlocks.JUNIPER_DOOR));
    public static final Item JUNIPER_TRAPDOOR = named("juniper_trapdoor", new ItemBlock(PSBlocks.JUNIPER_TRAPDOOR));
    public static final Item JUNIPER_BUTTON = named("juniper_button", new ItemBlock(PSBlocks.JUNIPER_BUTTON));
    public static final Item JUNIPER_PRESSURE_PLATE = named("juniper_pressure_plate", new ItemBlock(PSBlocks.JUNIPER_PRESSURE_PLATE));
    public static final Item JUNIPER_SIGN = named("juniper_sign", new JuniperSignItem(false));
    public static final Item JUNIPER_HANGING_SIGN = named("juniper_hanging_sign", new JuniperSignItem(true));
    public static final Item IRON_CHAIN = named("iron_chain", new Item());
    public static final Item GLOWING_INK = named("glowing_ink", new Item());
    public static final Item SIGN_WAX = named("sign_wax", new Item());
    public static final Item SIGN_BLACK_DYE = named("sign_black_dye", new Item());
    public static final Item JUNIPER_BERRIES = named("juniper_berries", new QuickFoodItem(1, 0.5F, true));
    public static final Item DRYING_TABLE = named("drying_table", new ItemBlock(PSBlocks.DRYING_TABLE));
    public static final Item IRON_DRYING_TABLE = named("iron_drying_table", new ItemBlock(PSBlocks.IRON_DRYING_TABLE));

    private PSItems() {
    }

    private static Item edible(String name, int hunger, float saturation, DrugType type) {
        return named(name, new EdibleItem(hunger, saturation,
                () -> new DrugInfluence(type, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5F)));
    }

    private static Item named(String name, Item item) {
        item.setRegistryName(Psychedelicraft.id(name))
                .setUnlocalizedName(Psychedelicraft.MOD_ID + "." + name)
                .setCreativeTab(GENERAL);
        ITEMS.add(item);
        return item;
    }

    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ITEMS) {
            event.getRegistry().register(item);
        }
    }

    @SubscribeEvent
    public static void furnaceFuel(FurnaceFuelBurnTimeEvent event) {
        Item item = event.getItemStack().getItem();
        if (item == LATTICE) {
            event.setBurnTime(700);
        } else if (item == MORNING_GLORY_LATTICE || item == WINE_GRAPE_LATTICE) {
            event.setBurnTime(0);
        } else if (item == JUNIPER_SAPLING || item == JUNIPER_BUTTON) {
            event.setBurnTime(100);
        } else if (item == JUNIPER_SLAB) {
            event.setBurnTime(150);
        } else if (item == JUNIPER_DOOR || item == JUNIPER_SIGN) {
            event.setBurnTime(200);
        } else if (item == JUNIPER_HANGING_SIGN) {
            event.setBurnTime(800);
        }
    }
}
