/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.block.entity.FlaskBlockEntity;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.fluid.FluidVessel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSItems {
    private static final List<Item> ITEMS = new ArrayList<>();
    public static final Item COFFEE_BEANS = named("coffee_beans", new Item());
    public static final PortableFluidItem WOODEN_MUG = vessel("wooden_mug", FluidVessel.MUG);
    public static final PortableFluidItem STONE_CUP = vessel("stone_cup", FluidVessel.CUP);
    public static final PortableFluidItem GLASS_CHALICE = vessel("glass_chalice", FluidVessel.CHALICE);
    public static final PortableFluidItem SHOT_GLASS = vessel("shot_glass", FluidVessel.SHOT_GLASS);
    public static final PortableFluidItem BOTTLE = vessel("bottle", FluidVessel.BOTTLE);
    public static final MolotovCocktailItem MOLOTOV_COCKTAIL = named("molotov_cocktail", new MolotovCocktailItem(), PSItemGroups.WEAPONS);
    public static final PortableFluidItem SYRINGE = vessel("syringe", FluidVessel.SYRINGE);
    public static final PortableFluidItem FILLED_BOWL = vessel("filled_bowl", FluidVessel.BOWL);
    public static final PortableFluidItem FILLED_GLASS_BOTTLE = vessel("filled_glass_bottle", FluidVessel.GLASS_BOTTLE);
    public static final HoneyBottleItem HONEY_BOTTLE = named("honey_bottle", new HoneyBottleItem());
    public static final Item HONEYCOMB = named("honeycomb", new Item());
    public static final Item BEE_NEST = named("bee_nest", new ItemBlock(PSBlocks.BEE_NEST));
    public static final Item CAMPFIRE = named("campfire", new WaterloggableBlockItem(PSBlocks.CAMPFIRE));
    public static final Item SOUL_CAMPFIRE = named("soul_campfire", new WaterloggableBlockItem(PSBlocks.SOUL_CAMPFIRE));
    public static final Item SOUL_SOIL = named("soul_soil", new ItemBlock(PSBlocks.SOUL_SOIL));
    public static final Item KELP = named("kelp", new ItemBlock(PSBlocks.KELP));
    public static final Item DRIED_KELP = named("dried_kelp", new QuickFoodItem(1, 0.3F, false, 16));
    public static final Item DRIED_KELP_BLOCK = named("dried_kelp_block", new ItemBlock(PSBlocks.DRIED_KELP_BLOCK));
    public static final Item SWEET_BERRIES = named("sweet_berries", new SweetBerriesItem());
    public static final Item TORCHFLOWER_SEEDS = named("torchflower_seeds", new CropSeedItem(PSBlocks.TORCHFLOWER_CROP, "torchflower_seeds"));
    public static final Item TORCHFLOWER = named("torchflower", new ItemBlock(PSBlocks.TORCHFLOWER));
    public static final Item CORNFLOWER = named("cornflower", new ItemBlock(PSBlocks.CORNFLOWER));
    public static final Item LILY_OF_THE_VALLEY = named("lily_of_the_valley", new ItemBlock(PSBlocks.LILY_OF_THE_VALLEY));
    public static final Item WITHER_ROSE = named("wither_rose", new ItemBlock(PSBlocks.WITHER_ROSE));
    public static final SuspiciousStewItem SUSPICIOUS_STEW = named("suspicious_stew", new SuspiciousStewItem());
    public static final Item PITCHER_POD = named("pitcher_pod", new CropSeedItem(PSBlocks.PITCHER_CROP, "pitcher_pod"));
    public static final Item PITCHER_PLANT = named("pitcher_plant", new ItemBlock(PSBlocks.PITCHER_PLANT));
    public static final Item BRUSH = named("brush", new BrushItem());
    public static final Item SUSPICIOUS_SAND = named("suspicious_sand", new ItemBlock(PSBlocks.SUSPICIOUS_SAND));
    public static final Item SNIFFER_EGG = named("sniffer_egg", new ItemBlock(PSBlocks.SNIFFER_EGG));
    public static final Item MOSS_BLOCK = named("moss_block", new ItemBlock(PSBlocks.MOSS_BLOCK));
    public static final Item MOSS_CARPET = named("moss_carpet", new ItemBlock(PSBlocks.MOSS_CARPET));
    public static final Item AZALEA = named("azalea", new ItemBlock(PSBlocks.AZALEA));
    public static final Item FLOWERING_AZALEA = named("flowering_azalea", new ItemBlock(PSBlocks.FLOWERING_AZALEA));
    public static final Item AZALEA_LEAVES = named("azalea_leaves", new ItemBlock(PSBlocks.AZALEA_LEAVES));
    public static final Item FLOWERING_AZALEA_LEAVES = named("flowering_azalea_leaves", new ItemBlock(PSBlocks.FLOWERING_AZALEA_LEAVES));
    public static final Item ROOTED_DIRT = named("rooted_dirt", new ItemBlock(PSBlocks.ROOTED_DIRT));
    public static final Item HANGING_ROOTS = named("hanging_roots", new ItemBlock(PSBlocks.HANGING_ROOTS));
    public static final Item MUD = named("mud", new ItemBlock(PSBlocks.MUD));
    public static final Item ANGLER_POTTERY_SHERD = named("angler_pottery_sherd", new Item());
    public static final Item SHELTER_POTTERY_SHERD = named("shelter_pottery_sherd", new Item());
    public static final Item SNORT_POTTERY_SHERD = named("snort_pottery_sherd", new Item());
    public static final FilledBucketItem FILLED_BUCKET = named("filled_bucket", new FilledBucketItem(), PSItemGroups.DRINKS);
    public static final Item BROWN_MAGIC_MUSHROOMS = edible("brown_magic_mushrooms", 0, 0.1F, DrugType.BROWN_SHROOMS);
    public static final Item RED_MAGIC_MUSHROOMS = edible("red_magic_mushrooms", 0, 0.1F, DrugType.RED_SHROOMS);
    public static final Item CANNABIS_SEEDS = named("cannabis_seeds", new CropSeedItem(PSBlocks.CANNABIS, "cannabis_seeds"));
    public static final Item CANNABIS_LEAF = named("cannabis_leaf", new Item());
    public static final Item CANNABIS_BUDS = named("cannabis_buds", new Item());
    public static final Item DRIED_CANNABIS_LEAF = named("dried_cannabis_leaf", new Item());
    public static final Item DRIED_CANNABIS_BUDS = named("dried_cannabis_buds", new Item());
    public static final Item HASH_MUFFIN = edible("hash_muffin", 2, 0.1F,
            new DrugInfluence(DrugType.CANNABIS, DrugInfluence.METABOLISED_DELAY, 0.004, 0.002, 0.7));
    public static final Item JOLLY_RANCHER = edible("jolly_rancher", 0, 0.1F,
            new DrugInfluence(DrugType.SUGAR, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.05));
    public static final SmokeableItem JOINT = named("joint", new SmokeableItem(2, 20, 0xe6e6e6,
            new DrugInfluence(DrugType.CANNABIS, DrugInfluence.INHALED_DELAY, 0.002, 0.001, 0.20)).stackable());
    public static final Item HOP_SEEDS = named("hop_seeds", new CropSeedItem(PSBlocks.HOP, "hop_seeds"));
    public static final Item HOP_CONES = named("hop_cones", new Item());
    public static final Item COCA_SEEDS = named("coca_seeds", new CropSeedItem(PSBlocks.COCA, "coca_seeds"));
    public static final Item COCA_LEAVES = named("coca_leaves", new Item());
    public static final Item DRIED_COCA_LEAVES = named("dried_coca_leaves", new Item());
    public static final Item COCAINE_POWDER = named("cocaine_powder", new PowderItem(
            new DrugInfluence(DrugType.COCAINE, DrugInfluence.IMMEDIATE, 0.002, 0.003, 0.35)));
    public static final Item OBSIDIAN_BOTTLE = named("obsidian_bottle", new Item().setMaxStackSize(16));
    public static final Item OBSIDIAN_DUST = named("obsidian_dust", new PowderItem(
            new DrugInfluence(DrugType.BATH_SALTS, DrugInfluence.IMMEDIATE, 0.002, 0.003, 0.35)));
    public static final Item TOBACCO_SEEDS = named("tobacco_seeds", new CropSeedItem(PSBlocks.TOBACCO, "tobacco_seeds"));
    public static final Item TOBACCO_LEAVES = named("tobacco", new Item());
    public static final Item DRIED_TOBACCO = named("dried_tobacco", new Item());
    public static final SmokeableItem CIGARETTE = named("cigarette", new SmokeableItem(1, 50, 0xffffff,
            new DrugInfluence(DrugType.TOBACCO, DrugInfluence.IMMEDIATE, 0.1, 0.02, 0.7)));
    public static final SmokeableItem CIGAR = named("cigar", new SmokeableItem(3, 80, 0x999980, 4F,
            new DrugInfluence(DrugType.TOBACCO, DrugInfluence.IMMEDIATE, 0.1, 0.02, 0.7)));
    public static final Item COFFEA_CHERRIES = named("coffea_cherries", new CropSeedItem(PSBlocks.COFFEA, "coffea_cherries"));
    public static final Item PEYOTE = named("peyote", new ItemBlock(PSBlocks.PEYOTE));
    public static final Item DRIED_PEYOTE = edible("dried_peyote", 0, 0.1F, DrugType.PEYOTE);
    public static final SmokeableItem PEYOTE_JOINT = named("peyote_joint", new SmokeableItem(2, 20, 0x80e666,
            new DrugInfluence(DrugType.PEYOTE, DrugInfluence.INHALED_DELAY, 0.003, 0.0015, 0.4),
            new DrugInfluence(DrugType.TOBACCO, DrugInfluence.IMMEDIATE, 0.1, 0.02, 0.1)).stackable());
    public static final Item AGAVE_LEAF = named("agave_leaf", new CropSeedItem(PSBlocks.AGAVE_PLANT, "agave_leaf"));
    public static final Item JIMSONWEED_SEEDS = named("jimsonweed_seeds", new CropSeedItem(PSBlocks.JIMSONWEED, "jimsonweed_seeds"));
    public static final Item JIMSONWEED_SEED_POD = named("jimsonweed_seed_pod", new Item());
    public static final Item JIMSONWEED_LEAF = named("jimsonweed_leaf", new Item());
    public static final Item DRIED_JIMSONWEED_LEAF = named("dried_jimsonweed_leaf", new Item());
    public static final Item BELLADONNA_SEEDS = named("belladonna_seeds", new CropSeedItem(PSBlocks.BELLADONNA, "belladonna_seeds"));
    public static final Item BELLADONNA_BERRIES = edible("belladonna_berries", 1, 1.5F, DrugType.ATROPINE);
    public static final Item BELLADONNA_LEAF = named("belladonna_leaf", new Item());
    public static final Item DRIED_BELLADONNA_LEAF = named("dried_belladonna_leaf", new Item());
    public static final HarmoniumItem HARMONIUM = named("harmonium", new HarmoniumItem());
    public static final BongItem SMOKING_PIPE = named("smoking_pipe", new BongItem(50, 200, false))
            .consumes(DRIED_CANNABIS_BUDS, new DrugInfluence(DrugType.CANNABIS, DrugInfluence.INHALED_DELAY, 0.002, 0.001, 0.25))
            .consumes(DRIED_TOBACCO, new DrugInfluence(DrugType.TOBACCO, DrugInfluence.INHALED_DELAY, 0.1, 0.02, 0.8))
            .consumes(DRIED_BELLADONNA_LEAF, new DrugInfluence(DrugType.ATROPINE, DrugInfluence.INHALED_DELAY, 0.4, 0.1, 0.9))
            .consumes(DRIED_JIMSONWEED_LEAF, new DrugInfluence(DrugType.ATROPINE, DrugInfluence.INHALED_DELAY, 0.5, 0.1, 0.2))
            .consumes(HARMONIUM, stack -> DrugInfluence.harmonium(DrugInfluence.INHALED_DELAY, 0.65, HarmoniumItem.getColor(stack)));
    public static final BongItem BONG = named("bong", new BongItem(128, 0, true))
            .consumes(DRIED_CANNABIS_BUDS, new DrugInfluence(DrugType.CANNABIS, DrugInfluence.IMMEDIATE, 0.002, 0.001, 0.2))
            .consumes(DRIED_TOBACCO, new DrugInfluence(DrugType.TOBACCO, DrugInfluence.IMMEDIATE, 0.1, 0.02, 0.6))
            .consumes(DRIED_BELLADONNA_LEAF, new DrugInfluence(DrugType.ATROPINE, DrugInfluence.IMMEDIATE, 0.4, 0.1, 0.4))
            .consumes(DRIED_JIMSONWEED_LEAF, new DrugInfluence(DrugType.ATROPINE, DrugInfluence.IMMEDIATE, 0.5, 0.1, 0.1))
            .consumes(HARMONIUM, stack -> DrugInfluence.harmonium(DrugInfluence.IMMEDIATE, 0.9, HarmoniumItem.getColor(stack)));
    public static final Item TOMATO_SEEDS = named("tomato_seeds", new CropSeedItem(PSBlocks.TOMATOES, "tomato_seeds"));
    public static final Item TOMATO = named("tomato", new EdibleItem(1, 1.9F));
    public static final Item TOMATO_LEAF = named("tomato_leaf", new Item());
    public static final Item LATTICE = named("lattice", new WaterloggableBlockItem(PSBlocks.LATTICE));
    public static final Item MORNING_GLORY = named("morning_glory", new Item());
    public static final Item LSA_SQUARE = edible("lsd_square", 0, 0.1F,
            new DrugInfluence(DrugType.LSD, DrugInfluence.CONTACT_DELAY, 0.05, 0.003, 0.1));
    public static final Item LSD_PILL = edible("lsd_pill", 0, 0.1F,
            new DrugInfluence(DrugType.LSD, DrugInfluence.INGESTED_DELAY, 0.05, 0.003, 0.6));
    public static final Item MORNING_GLORY_SEEDS = named("morning_glory_seeds",
            new CropSeedItem(PSBlocks.MORNING_GLORY, "morning_glory_seeds"));
    public static final Item MORNING_GLORY_LATTICE = named("morning_glory_lattice",
            new LatticeCropItem(PSBlocks.MORNING_GLORY_LATTICE), null);
    public static final Item WINE_GRAPE_LATTICE = named("wine_grape_lattice",
            new LatticeCropItem(PSBlocks.WINE_GRAPE_LATTICE), null);
    public static final Item WINE_GRAPES = named("wine_grapes", new WineGrapesItem());
    public static final Item JUNIPER_LOG = named("juniper_log", new ItemBlock(PSBlocks.JUNIPER_LOG));
    public static final Item JUNIPER_WOOD = named("juniper_wood", new ItemBlock(PSBlocks.JUNIPER_WOOD));
    public static final Item STRIPPED_JUNIPER_LOG = named("stripped_juniper_log", new ItemBlock(PSBlocks.STRIPPED_JUNIPER_LOG));
    public static final Item STRIPPED_JUNIPER_WOOD = named("stripped_juniper_wood", new ItemBlock(PSBlocks.STRIPPED_JUNIPER_WOOD));
    public static final Item JUNIPER_LEAVES = named("juniper_leaves", new WaterloggableBlockItem(PSBlocks.JUNIPER_LEAVES));
    public static final Item FRUITING_JUNIPER_LEAVES = named("fruiting_juniper_leaves", new WaterloggableBlockItem(PSBlocks.FRUITING_JUNIPER_LEAVES));
    public static final Item JUNIPER_SAPLING = named("juniper_sapling", new ItemBlock(PSBlocks.JUNIPER_SAPLING));
    public static final Item JUNIPER_PLANKS = named("juniper_planks", new ItemBlock(PSBlocks.JUNIPER_PLANKS));
    public static final Item JUNIPER_STAIRS = named("juniper_stairs", new WaterloggableBlockItem(PSBlocks.JUNIPER_STAIRS));
    public static final Item JUNIPER_SLAB = named("juniper_slab", new JuniperSlabItem());
    public static final Item JUNIPER_FENCE = named("juniper_fence", new WaterloggableBlockItem(PSBlocks.JUNIPER_FENCE));
    public static final Item JUNIPER_FENCE_GATE = named("juniper_fence_gate", new ItemBlock(PSBlocks.JUNIPER_FENCE_GATE));
    public static final Item JUNIPER_DOOR = named("juniper_door", new ItemDoor(PSBlocks.JUNIPER_DOOR));
    public static final Item JUNIPER_TRAPDOOR = named("juniper_trapdoor", new WaterloggableBlockItem(PSBlocks.JUNIPER_TRAPDOOR));
    public static final Item JUNIPER_BUTTON = named("juniper_button", new ItemBlock(PSBlocks.JUNIPER_BUTTON));
    public static final Item JUNIPER_PRESSURE_PLATE = named("juniper_pressure_plate", new ItemBlock(PSBlocks.JUNIPER_PRESSURE_PLATE));
    public static final Item JUNIPER_SIGN = named("juniper_sign", new JuniperSignItem(false));
    public static final Item JUNIPER_HANGING_SIGN = named("juniper_hanging_sign", new JuniperSignItem(true));
    public static final JuniperBoatItem JUNIPER_BOAT = named("juniper_boat", new JuniperBoatItem(false));
    public static final JuniperBoatItem JUNIPER_CHEST_BOAT = named("juniper_chest_boat", new JuniperBoatItem(true));
    public static final Item IRON_CHAIN = named("iron_chain", new Item());
    public static final Item GLOWING_INK = named("glowing_ink", new Item());
    public static final Item SIGN_WAX = named("sign_wax", new Item());
    public static final Item SIGN_BLACK_DYE = named("sign_black_dye", new Item());
    public static final Item JUNIPER_BERRIES = named("juniper_berries", new QuickFoodItem(1, 0.5F, true));
    public static final Item DRYING_TABLE = named("drying_table", new ItemBlock(PSBlocks.DRYING_TABLE));
    public static final Item IRON_DRYING_TABLE = named("iron_drying_table", new ItemBlock(PSBlocks.IRON_DRYING_TABLE));
    public static final MashTubItem MASH_TUB = named("mash_tub", new MashTubItem());
    public static final FluidMachineItem OAK_BARREL = barrel(PSBlocks.OAK_BARREL);
    public static final FluidMachineItem SPRUCE_BARREL = barrel(PSBlocks.SPRUCE_BARREL);
    public static final FluidMachineItem BIRCH_BARREL = barrel(PSBlocks.BIRCH_BARREL);
    public static final FluidMachineItem JUNGLE_BARREL = barrel(PSBlocks.JUNGLE_BARREL);
    public static final FluidMachineItem ACACIA_BARREL = barrel(PSBlocks.ACACIA_BARREL);
    public static final FluidMachineItem DARK_OAK_BARREL = barrel(PSBlocks.DARK_OAK_BARREL);
    public static final FluidMachineItem FLASK = named("flask", new FluidMachineItem(PSBlocks.FLASK, FlaskBlockEntity.CAPACITY));
    public static final FluidMachineItem DISTILLERY = named("distillery", new FluidMachineItem(PSBlocks.DISTILLERY, FlaskBlockEntity.CAPACITY));
    public static final Item BOTTLE_RACK = named("bottle_rack", new ItemBlock(PSBlocks.BOTTLE_RACK));
    public static final Item TRAY = named("tray", new ItemBlock(PSBlocks.TRAY));
    public static final Item BUNSEN_BURNER = named("bunsen_burner", new ItemBlock(PSBlocks.BUNSEN_BURNER));
    public static final RiftJarItem RIFT_JAR = named("rift_jar", new RiftJarItem(PSBlocks.RIFT_JAR));
    public static final Item COPPER_ORE = named("copper_ore", new ItemBlock(PSBlocks.COPPER_ORE));
    public static final Item RAW_COPPER = named("raw_copper", new Item());
    public static final Item COPPER_INGOT = named("copper_ingot", new Item());
    public static final Item HEART_OF_THE_SEA = named("heart_of_the_sea", new Item());
    public static final Item NAUTILUS_SHELL = named("nautilus_shell", new Item());
    public static final Item CONDUIT = named("conduit", new WaterloggableBlockItem(PSBlocks.CONDUIT));
    public static final Item VOMIT = named("vomit", new Item());
    public static final PaperBagItem PAPER_BAG = named("paper_bag", new PaperBagItem());
    public static final BagOfVomitItem BAG_O_VOMIT = named("bag_o_vomit", new BagOfVomitItem());

    private PSItems() {
    }

    private static FluidMachineItem barrel(BarrelBlock block) {
        return named(block.getRegistryName().getResourcePath(), new FluidMachineItem(block, BarrelBlockEntity.CAPACITY));
    }

    private static Item edible(String name, int hunger, float saturation, DrugType type) {
        return edible(name, hunger, saturation, new DrugInfluence(type, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5F));
    }

    private static Item edible(String name, int hunger, float saturation, DrugInfluence influence) {
        return named(name, new EdibleItem(hunger, saturation, influence::copy));
    }

    private static PortableFluidItem vessel(String name, FluidVessel vessel) {
        return named(name, new PortableFluidItem(vessel),
                vessel == FluidVessel.SYRINGE ? PSItemGroups.GENERAL : PSItemGroups.DRINKS);
    }

    private static <T extends Item> T named(String name, T item) {
        return named(name, item, PSItemGroups.GENERAL);
    }

    private static <T extends Item> T named(String name, T item, CreativeTabs tab) {
        item.setRegistryName(Psychedelicraft.id(name))
                .setUnlocalizedName(Psychedelicraft.MOD_ID + "." + name)
                .setCreativeTab(tab);
        if (item instanceof ItemBlock) ((ItemBlock) item).getBlock().setCreativeTab(tab);
        ITEMS.add(item);
        return item;
    }

    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static boolean isSnifferSeed(Item item) {
        return item == TORCHFLOWER_SEEDS || item == PITCHER_POD;
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
        } else if (item == WOODEN_MUG) {
            event.setBurnTime(50);
        } else if (item == MORNING_GLORY_LATTICE || item == WINE_GRAPE_LATTICE) {
            event.setBurnTime(0);
        } else if (item == DRIED_KELP_BLOCK) {
            event.setBurnTime(4000);
        } else if (item == JUNIPER_SAPLING || item == JUNIPER_BUTTON) {
            event.setBurnTime(100);
        } else if (item == JUNIPER_SLAB) {
            event.setBurnTime(150);
        } else if (item == JUNIPER_DOOR || item == JUNIPER_SIGN) {
            event.setBurnTime(200);
        } else if (item == JUNIPER_HANGING_SIGN) {
            event.setBurnTime(800);
        } else if (item == JUNIPER_BOAT || item == JUNIPER_CHEST_BOAT) {
            event.setBurnTime(1200);
        }
    }
}
