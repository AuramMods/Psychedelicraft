package ivorius.psychedelicraft.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.TallCropBlock.Crop;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.block.entity.BeeNestBlockEntity;
import ivorius.psychedelicraft.block.entity.CampfireBlockEntity;
import ivorius.psychedelicraft.block.entity.BottleRackBlockEntity;
import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.block.entity.FlaskBlockEntity;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import ivorius.psychedelicraft.block.entity.LatticeCropBlockEntity;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.block.entity.PeyoteBlockEntity;
import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.block.entity.WaterloggedBlockEntity;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.physical.FluidAttributesTile;
import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSBlocks {
    public static final TallCropBlock CANNABIS = named("cannabis", new TallCropBlock(Crop.CANNABIS, false));
    public static final TallCropBlock HOP = named("hop", new TallCropBlock(Crop.HOP, false));
    public static final TallCropBlock COCA = named("coca", new TallCropBlock.Coca(false));
    public static final TallCropBlock TOBACCO = named("tobacco", new TallCropBlock.TwoPart(Crop.TOBACCO, false));
    public static final TallCropBlock COFFEA = named("coffea", new TallCropBlock.TwoPart(Crop.COFFEA, false));
    // Age and upper-section state can use all four metadata bits.
    public static final TallCropBlock NATURAL_CANNABIS = named("natural_cannabis", new TallCropBlock(Crop.CANNABIS, true));
    public static final TallCropBlock NATURAL_HOP = named("natural_hop", new TallCropBlock(Crop.HOP, true));
    public static final TallCropBlock NATURAL_COCA = named("natural_coca", new TallCropBlock.Coca(true));
    public static final TallCropBlock NATURAL_TOBACCO = named("natural_tobacco", new TallCropBlock.TwoPart(Crop.TOBACCO, true));
    public static final TallCropBlock NATURAL_COFFEA = named("natural_coffea", new TallCropBlock.TwoPart(Crop.COFFEA, true));
    public static final PeyoteBlock PEYOTE = named("peyote", new PeyoteBlock());
    public static final AgavePlantBlock AGAVE_PLANT = named("agave_plant", new AgavePlantBlock());
    public static final NightshadeBlock JIMSONWEED = named("jimsonweed",
            new NightshadeBlock(() -> PSItems.JIMSONWEED_SEED_POD, () -> PSItems.JIMSONWEED_LEAF));
    public static final NightshadeBlock BELLADONNA = named("belladonna",
            new NightshadeBlock(() -> PSItems.BELLADONNA_BERRIES, () -> PSItems.BELLADONNA_LEAF));
    public static final NightshadeBlock TOMATOES = named("tomatoes",
            new NightshadeBlock(() -> PSItems.TOMATO, () -> PSItems.TOMATO_LEAF));
    public static final LatticeBlock LATTICE = named("lattice", new LatticeBlock());
    public static final VineStemBlock MORNING_GLORY = named("morning_glory", new VineStemBlock());
    public static final LatticeCropBlock MORNING_GLORY_LATTICE = named("morning_glory_lattice",
            new LatticeCropBlock(true, 2, () -> PSItems.MORNING_GLORY));
    public static final LatticeCropBlock WINE_GRAPE_LATTICE = named("wine_grape_lattice",
            new LatticeCropBlock(false, 1, () -> PSItems.WINE_GRAPES));
    public static final JuniperLogBlock STRIPPED_JUNIPER_LOG = named("stripped_juniper_log", new JuniperLogBlock(null));
    public static final JuniperLogBlock STRIPPED_JUNIPER_WOOD = named("stripped_juniper_wood", new JuniperLogBlock(null));
    public static final JuniperLogBlock JUNIPER_LOG = named("juniper_log", new JuniperLogBlock(() -> STRIPPED_JUNIPER_LOG));
    public static final JuniperLogBlock JUNIPER_WOOD = named("juniper_wood", new JuniperLogBlock(() -> STRIPPED_JUNIPER_WOOD));
    public static final JuniperLeavesBlock JUNIPER_LEAVES = named("juniper_leaves", new JuniperLeavesBlock(false));
    public static final JuniperLeavesBlock FRUITING_JUNIPER_LEAVES = named("fruiting_juniper_leaves", new JuniperLeavesBlock(true));
    public static final JuniperSaplingBlock JUNIPER_SAPLING = named("juniper_sapling", new JuniperSaplingBlock());
    public static final Block JUNIPER_PLANKS = named("juniper_planks", new JuniperPlanksBlock());
    public static final BlockStairs JUNIPER_STAIRS = named("juniper_stairs",
            new JuniperBuildingBlocks.Stairs(JUNIPER_PLANKS.getDefaultState()));
    public static final BlockSlab JUNIPER_SLAB = named("juniper_slab", new JuniperSlabBlock.Single());
    public static final BlockSlab JUNIPER_DOUBLE_SLAB = named("juniper_double_slab", new JuniperSlabBlock.Double());
    public static final BlockFence JUNIPER_FENCE = named("juniper_fence", new JuniperBuildingBlocks.Fence());
    public static final BlockFenceGate JUNIPER_FENCE_GATE = named("juniper_fence_gate", new JuniperBuildingBlocks.Gate());
    public static final BlockDoor JUNIPER_DOOR = named("juniper_door", new JuniperBuildingBlocks.Door());
    public static final BlockTrapDoor JUNIPER_TRAPDOOR = named("juniper_trapdoor", new JuniperTrapDoorBlock(false));
    public static final BlockTrapDoor POWERED_JUNIPER_TRAPDOOR = named("powered_juniper_trapdoor", new JuniperTrapDoorBlock(true));
    public static final BlockButtonWood JUNIPER_BUTTON = named("juniper_button", new JuniperButtonBlock());
    public static final BlockPressurePlate JUNIPER_PRESSURE_PLATE = named("juniper_pressure_plate",
            new JuniperBuildingBlocks.PressurePlate());
    public static final JuniperSignBlock JUNIPER_SIGN = named("juniper_sign", new JuniperSignBlock.Standing());
    public static final JuniperSignBlock JUNIPER_WALL_SIGN = named("juniper_wall_sign", new JuniperSignBlock.Wall());
    public static final JuniperSignBlock JUNIPER_HANGING_SIGN = named("juniper_hanging_sign", new JuniperHangingSigns.Ceiling());
    public static final JuniperSignBlock JUNIPER_WALL_HANGING_SIGN = named("juniper_wall_hanging_sign", new JuniperHangingSigns.Wall());
    static final List<PottedPlantBlock> POTTED_PLANTS = Collections.unmodifiableList(Arrays.asList(
            named("potted_morning_glory", new PottedPlantBlock(() -> PSItems.MORNING_GLORY_SEEDS)),
            named("potted_juniper_sapling", new PottedPlantBlock(() -> PSItems.JUNIPER_SAPLING)),
            named("potted_cannabis", new PottedPlantBlock(() -> PSItems.CANNABIS_SEEDS)),
            named("potted_hop", new PottedPlantBlock(() -> PSItems.HOP_SEEDS)),
            named("potted_tobacco", new PottedPlantBlock(() -> PSItems.TOBACCO_SEEDS)),
            named("potted_coca", new PottedPlantBlock(() -> PSItems.COCA_SEEDS)),
            named("potted_coffea", new PottedPlantBlock(() -> PSItems.COFFEA_CHERRIES))));
    public static final DryingTableBlock DRYING_TABLE = named("drying_table", new DryingTableBlock(Material.WOOD));
    public static final DryingTableBlock IRON_DRYING_TABLE = named("iron_drying_table", new DryingTableBlock(Material.IRON));
    public static final MashTubBlock MASH_TUB = named("mash_tub", new MashTubBlock(false));
    public static final MashTubBlock MASH_TUB_EDGE = named("mash_tub_edge", new MashTubBlock(true));
    public static final BarrelBlock OAK_BARREL = named("oak_barrel", new BarrelBlock());
    public static final BarrelBlock SPRUCE_BARREL = named("spruce_barrel", new BarrelBlock());
    public static final BarrelBlock BIRCH_BARREL = named("birch_barrel", new BarrelBlock());
    public static final BarrelBlock JUNGLE_BARREL = named("jungle_barrel", new BarrelBlock());
    public static final BarrelBlock ACACIA_BARREL = named("acacia_barrel", new BarrelBlock());
    public static final BarrelBlock DARK_OAK_BARREL = named("dark_oak_barrel", new BarrelBlock());
    public static final FlaskBlock FLASK = named("flask", new FlaskBlock());
    public static final DistilleryBlock DISTILLERY = named("distillery", new DistilleryBlock());
    public static final PlacedDrinksBlock PLACED_DRINK = named("placed_drink", new PlacedDrinksBlock());
    public static final BottleRackBlock BOTTLE_RACK = named("bottle_rack", new BottleRackBlock());
    public static final Block TRAY = named("tray", new LabPropBlock.Tray());
    public static final Block BUNSEN_BURNER = named("bunsen_burner", LabPropBlock.burner());
    public static final RiftJarBlock RIFT_JAR = named("rift_jar", new RiftJarBlock());
    public static final Block GLITCH = named("glitch", new GlitchedBlock());
    public static final Block COPPER_ORE = named("copper_ore", new CopperOreBlock());
    public static final BeeNestBlock BEE_NEST = named("bee_nest", new BeeNestBlock());
    public static final CampfireBlock CAMPFIRE = named("campfire", new CampfireBlock());
    public static final KelpBlock KELP = named("kelp", new KelpBlock(false));
    public static final KelpBlock KELP_STALK = named("kelp_stalk", new KelpBlock(true));
    public static final Block DRIED_KELP_BLOCK = named("dried_kelp_block", new DriedKelpBlock());
    public static final BubbleColumnBlock BUBBLE_COLUMN = named("bubble_column", new BubbleColumnBlock());

    private PSBlocks() {
    }

    private static <T extends Block> T named(String name, T block) {
        block.setRegistryName(Psychedelicraft.id(name));
        block.setUnlocalizedName(Psychedelicraft.MOD_ID + "." + name);
        return block;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(CANNABIS, NATURAL_CANNABIS, HOP, COCA, TOBACCO, COFFEA,
                PEYOTE, AGAVE_PLANT, JIMSONWEED, BELLADONNA, TOMATOES, DRYING_TABLE, IRON_DRYING_TABLE,
                MASH_TUB, MASH_TUB_EDGE,
                OAK_BARREL, SPRUCE_BARREL, BIRCH_BARREL, JUNGLE_BARREL, ACACIA_BARREL, DARK_OAK_BARREL,
                FLASK, DISTILLERY, PLACED_DRINK, BOTTLE_RACK, TRAY, BUNSEN_BURNER, RIFT_JAR, GLITCH, COPPER_ORE,
                LATTICE, MORNING_GLORY, MORNING_GLORY_LATTICE, WINE_GRAPE_LATTICE,
                NATURAL_HOP, NATURAL_COCA, NATURAL_TOBACCO, NATURAL_COFFEA,
                JUNIPER_LOG, JUNIPER_WOOD, STRIPPED_JUNIPER_LOG, STRIPPED_JUNIPER_WOOD,
                JUNIPER_LEAVES, FRUITING_JUNIPER_LEAVES, JUNIPER_SAPLING, JUNIPER_PLANKS,
                JUNIPER_STAIRS, JUNIPER_SLAB, JUNIPER_DOUBLE_SLAB, JUNIPER_FENCE, JUNIPER_FENCE_GATE,
                JUNIPER_DOOR, JUNIPER_TRAPDOOR, POWERED_JUNIPER_TRAPDOOR, JUNIPER_BUTTON, JUNIPER_PRESSURE_PLATE,
                JUNIPER_SIGN, JUNIPER_WALL_SIGN, JUNIPER_HANGING_SIGN, JUNIPER_WALL_HANGING_SIGN, BEE_NEST, BUBBLE_COLUMN,
                CAMPFIRE, KELP, KELP_STALK, DRIED_KELP_BLOCK);
        for (PottedPlantBlock pot : POTTED_PLANTS) {
            event.getRegistry().register(pot);
        }
        for (Fluid fluid : PSFluids.all()) {
            String name = PSFluids.id(fluid);
            event.getRegistry().register(named(name.substring(name.indexOf(':') + 1), new PhysicalFluidBlock(fluid)));
        }
        GameRegistry.registerTileEntity(FluidAttributesTile.class, Psychedelicraft.id("fluid_attributes"));
        GameRegistry.registerTileEntity(DryingTableBlockEntity.class, Psychedelicraft.id("drying_table"));
        GameRegistry.registerTileEntity(MashTubBlockEntity.class, Psychedelicraft.id("mash_tub"));
        GameRegistry.registerTileEntity(MashTubBlock.Edge.class, Psychedelicraft.id("mash_tub_edge"));
        GameRegistry.registerTileEntity(BarrelBlockEntity.class, Psychedelicraft.id("barrel"));
        GameRegistry.registerTileEntity(FlaskBlockEntity.class, Psychedelicraft.id("flask"));
        GameRegistry.registerTileEntity(DistilleryBlockEntity.class, Psychedelicraft.id("distillery"));
        GameRegistry.registerTileEntity(PlacedDrinkBlockEntity.class, Psychedelicraft.id("placed_drink"));
        GameRegistry.registerTileEntity(BottleRackBlockEntity.class, Psychedelicraft.id("bottle_rack"));
        GameRegistry.registerTileEntity(LatticeCropBlockEntity.class, Psychedelicraft.id("lattice_crop"));
        GameRegistry.registerTileEntity(WaterloggedBlockEntity.class, Psychedelicraft.id("waterlogged_block"));
        GameRegistry.registerTileEntity(PeyoteBlockEntity.class, Psychedelicraft.id("peyote"));
        GameRegistry.registerTileEntity(JuniperSignBlockEntity.class, Psychedelicraft.id("juniper_sign"));
        GameRegistry.registerTileEntity(RiftJarBlockEntity.class, Psychedelicraft.id("rift_jar"));
        GameRegistry.registerTileEntity(BeeNestBlockEntity.class, Psychedelicraft.id("bee_nest"));
        GameRegistry.registerTileEntity(CampfireBlockEntity.class, Psychedelicraft.id("campfire"));
        Blocks.FIRE.setFireInfo(DRYING_TABLE, 5, 20);
        Blocks.FIRE.setFireInfo(BOTTLE_RACK, 5, 20);
        Blocks.FIRE.setFireInfo(LATTICE, 5, 20);
        Blocks.FIRE.setFireInfo(MORNING_GLORY_LATTICE, 5, 20);
        Blocks.FIRE.setFireInfo(WINE_GRAPE_LATTICE, 5, 20);
        Blocks.FIRE.setFireInfo(JUNIPER_LOG, 5, 5);
        Blocks.FIRE.setFireInfo(JUNIPER_WOOD, 5, 5);
        Blocks.FIRE.setFireInfo(STRIPPED_JUNIPER_LOG, 5, 5);
        Blocks.FIRE.setFireInfo(STRIPPED_JUNIPER_WOOD, 5, 5);
        Blocks.FIRE.setFireInfo(JUNIPER_LEAVES, 30, 60);
        Blocks.FIRE.setFireInfo(JUNIPER_PLANKS, 5, 20);
        Blocks.FIRE.setFireInfo(JUNIPER_STAIRS, 5, 20);
        Blocks.FIRE.setFireInfo(JUNIPER_SLAB, 5, 20);
        Blocks.FIRE.setFireInfo(JUNIPER_DOUBLE_SLAB, 5, 20);
        Blocks.FIRE.setFireInfo(JUNIPER_FENCE, 5, 20);
        Blocks.FIRE.setFireInfo(JUNIPER_FENCE_GATE, 5, 20);
        Blocks.FIRE.setFireInfo(BEE_NEST, 5, 20);
    }
}
