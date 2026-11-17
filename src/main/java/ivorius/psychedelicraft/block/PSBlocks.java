package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSBlocks {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Psychedelicraft.MOD_ID);
    public static final RegistryObject<CannabisPlantBlock> CANNABIS=BLOCKS.register("cannabis",()->new CannabisPlantBlock(plant()));
    public static final RegistryObject<HopPlantBlock> HOP=BLOCKS.register("hop",()->new HopPlantBlock(plant()));
    public static final RegistryObject<TobaccoPlantBlock> TOBACCO=BLOCKS.register("tobacco",()->new TobaccoPlantBlock(plant()));
    public static final RegistryObject<CocaPlantBlock> COCA=BLOCKS.register("coca",()->new CocaPlantBlock(plant()));
    public static final RegistryObject<CoffeaPlantBlock> COFFEA=BLOCKS.register("coffea",()->new CoffeaPlantBlock(plant()));
    public static final RegistryObject<PeyoteBlock> PEYOTE=BLOCKS.register("peyote",()->new PeyoteBlock(plant()));
    public static final RegistryObject<AgavePlantBlock> AGAVE_PLANT=BLOCKS.register("agave_plant",()->new AgavePlantBlock(plant()));
    public static final RegistryObject<NightshadeBlock> JIMSONWEED=BLOCKS.register("jimsonweed",()->new NightshadeBlock(plant(),()->PSItems.JIMSONWEED_SEED_POD.get(),()->PSItems.JIMSONWEED_LEAF.get()));
    public static final RegistryObject<NightshadeBlock> BELLADONNA=BLOCKS.register("belladonna",()->new NightshadeBlock(plant(),()->PSItems.BELLADONNA_BERRIES.get(),()->PSItems.BELLADONNA_LEAF.get()));
    public static final RegistryObject<NightshadeBlock> TOMATOES=BLOCKS.register("tomatoes",()->new NightshadeBlock(plant(),()->PSItems.TOMATO.get(),()->PSItems.TOMATO_LEAF.get()));
    public static final RegistryObject<DryingTableBlock> DRYING_TABLE=BLOCKS.register("drying_table",()->new DryingTableBlock(
            BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).ignitedByLava()));
    public static final RegistryObject<DryingTableBlock> IRON_DRYING_TABLE=BLOCKS.register("iron_drying_table",()->new DryingTableBlock(
            BlockBehaviour.Properties.of().strength(5).sound(SoundType.METAL)));
    public static final RegistryObject<FlaskBlock> FLASK=BLOCKS.register("flask",FlaskBlock::new);
    public static final RegistryObject<DistilleryBlock> DISTILLERY=BLOCKS.register("distillery",DistilleryBlock::new);
    public static final RegistryObject<BarrelBlock> OAK_BARREL=BLOCKS.register("oak_barrel",()->new BarrelBlock(Blocks.OAK_PLANKS));
    public static final RegistryObject<BarrelBlock> BIRCH_BARREL=BLOCKS.register("birch_barrel",()->new BarrelBlock(Blocks.BIRCH_PLANKS));
    public static final RegistryObject<BarrelBlock> SPRUCE_BARREL=BLOCKS.register("spruce_barrel",()->new BarrelBlock(Blocks.SPRUCE_PLANKS));
    public static final RegistryObject<BarrelBlock> ACACIA_BARREL=BLOCKS.register("acacia_barrel",()->new BarrelBlock(Blocks.ACACIA_PLANKS));
    public static final RegistryObject<BarrelBlock> JUNGLE_BARREL=BLOCKS.register("jungle_barrel",()->new BarrelBlock(Blocks.JUNGLE_PLANKS));
    public static final RegistryObject<BarrelBlock> DARK_OAK_BARREL=BLOCKS.register("dark_oak_barrel",()->new BarrelBlock(Blocks.DARK_OAK_PLANKS));
    public static final RegistryObject<MashTubBlock> MASH_TUB=BLOCKS.register("mash_tub",MashTubBlock::new);
    public static final RegistryObject<MashTubWallBlock> MASH_TUB_EDGE=BLOCKS.register("mash_tub_edge",MashTubWallBlock::new);
    public static final RegistryObject<PlacedDrinksBlock> PLACED_DRINK=BLOCKS.register("placed_drink",PlacedDrinksBlock::new);
    public static final RegistryObject<BottleRackBlock> BOTTLE_RACK=BLOCKS.register("bottle_rack",BottleRackBlock::new);
    public static final RegistryObject<LabPropBlock> BUNSEN_BURNER=BLOCKS.register("bunsen_burner",LabPropBlock::new);
    public static final RegistryObject<LabPropBlock.Tray> TRAY=BLOCKS.register("tray",LabPropBlock.Tray::new);
    public static final RegistryObject<LatticeBlock> LATTICE=BLOCKS.register("lattice",()->new LatticeBlock(lattice()));
    public static final RegistryObject<LatticeCropBlock> WINE_GRAPE_LATTICE=BLOCKS.register("wine_grape_lattice",()->new LatticeCropBlock(false,1,()->PSItems.WINE_GRAPES.get(),lattice()));
    public static final RegistryObject<LatticeCropBlock> MORNING_GLORY_LATTICE=BLOCKS.register("morning_glory_lattice",()->new LatticeCropBlock(true,2,()->PSItems.MORNING_GLORY.get(),lattice()));
    public static final RegistryObject<VineStemBlock> MORNING_GLORY=BLOCKS.register("morning_glory",()->new VineStemBlock(plant()));
    public static final RegistryObject<net.minecraft.world.level.block.FlowerPotBlock> POTTED_MORNING_GLORY=BLOCKS.register("potted_morning_glory",()->new net.minecraft.world.level.block.FlowerPotBlock(
            ()->(net.minecraft.world.level.block.FlowerPotBlock)Blocks.FLOWER_POT,MORNING_GLORY,BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));
    private static BlockBehaviour.Properties lattice(){return BlockBehaviour.Properties.of().strength(.3F).sound(SoundType.WOOD).noOcclusion().ignitedByLava();}
    public static final RegistryObject<JuniperLeavesBlock> JUNIPER_LEAVES=BLOCKS.register("juniper_leaves",()->new JuniperLeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
    public static final RegistryObject<JuniperLeavesBlock> FRUITING_JUNIPER_LEAVES=BLOCKS.register("fruiting_juniper_leaves",()->new JuniperLeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
    public static final RegistryObject<JuniperLogBlock> JUNIPER_LOG=BLOCKS.register("juniper_log",()->new JuniperLogBlock(log(),()->PSBlocks.STRIPPED_JUNIPER_LOG.get()));
    public static final RegistryObject<JuniperLogBlock> JUNIPER_WOOD=BLOCKS.register("juniper_wood",()->new JuniperLogBlock(log(),()->PSBlocks.STRIPPED_JUNIPER_WOOD.get()));
    public static final RegistryObject<net.minecraft.world.level.block.RotatedPillarBlock> STRIPPED_JUNIPER_LOG=BLOCKS.register("stripped_juniper_log",()->new net.minecraft.world.level.block.RotatedPillarBlock(log()));
    public static final RegistryObject<net.minecraft.world.level.block.RotatedPillarBlock> STRIPPED_JUNIPER_WOOD=BLOCKS.register("stripped_juniper_wood",()->new net.minecraft.world.level.block.RotatedPillarBlock(log()));
    public static final RegistryObject<JuniperSaplingBlock> JUNIPER_SAPLING=BLOCKS.register("juniper_sapling",()->new JuniperSaplingBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> JUNIPER_PLANKS=BLOCKS.register("juniper_planks",()->new Block(planks()));
    public static final RegistryObject<net.minecraft.world.level.block.StairBlock> JUNIPER_STAIRS=BLOCKS.register("juniper_stairs",()->new net.minecraft.world.level.block.StairBlock(()->JUNIPER_PLANKS.get().defaultBlockState(),planks()));
    public static final RegistryObject<net.minecraft.world.level.block.SlabBlock> JUNIPER_SLAB=BLOCKS.register("juniper_slab",()->new net.minecraft.world.level.block.SlabBlock(planks()));
    public static final RegistryObject<net.minecraft.world.level.block.FenceBlock> JUNIPER_FENCE=BLOCKS.register("juniper_fence",()->new net.minecraft.world.level.block.FenceBlock(planks()));
    public static final RegistryObject<net.minecraft.world.level.block.FenceGateBlock> JUNIPER_FENCE_GATE=BLOCKS.register("juniper_fence_gate",()->new net.minecraft.world.level.block.FenceGateBlock(planks(),PSWoodTypes.JUNIPER));
    public static final RegistryObject<net.minecraft.world.level.block.DoorBlock> JUNIPER_DOOR=BLOCKS.register("juniper_door",()->new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR),PSWoodTypes.JUNIPER_SET));
    public static final RegistryObject<net.minecraft.world.level.block.TrapDoorBlock> JUNIPER_TRAPDOOR=BLOCKS.register("juniper_trapdoor",()->new net.minecraft.world.level.block.TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_TRAPDOOR),PSWoodTypes.JUNIPER_SET));
    public static final RegistryObject<net.minecraft.world.level.block.PressurePlateBlock> JUNIPER_PRESSURE_PLATE=BLOCKS.register("juniper_pressure_plate",()->new net.minecraft.world.level.block.PressurePlateBlock(net.minecraft.world.level.block.PressurePlateBlock.Sensitivity.EVERYTHING,BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE),PSWoodTypes.JUNIPER_SET));
    public static final RegistryObject<net.minecraft.world.level.block.ButtonBlock> JUNIPER_BUTTON=BLOCKS.register("juniper_button",()->new net.minecraft.world.level.block.ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON),PSWoodTypes.JUNIPER_SET,30,true));
    public static final RegistryObject<JuniperSigns.Standing> JUNIPER_SIGN=BLOCKS.register("juniper_sign",()->new JuniperSigns.Standing(BlockBehaviour.Properties.copy(Blocks.OAK_SIGN)));
    public static final RegistryObject<JuniperSigns.Wall> JUNIPER_WALL_SIGN=BLOCKS.register("juniper_wall_sign",()->new JuniperSigns.Wall(BlockBehaviour.Properties.copy(Blocks.OAK_WALL_SIGN).dropsLike(JUNIPER_SIGN.get())));
    public static final RegistryObject<JuniperSigns.Hanging> JUNIPER_HANGING_SIGN=BLOCKS.register("juniper_hanging_sign",()->new JuniperSigns.Hanging(BlockBehaviour.Properties.copy(Blocks.OAK_HANGING_SIGN)));
    public static final RegistryObject<JuniperSigns.HangingWall> JUNIPER_WALL_HANGING_SIGN=BLOCKS.register("juniper_wall_hanging_sign",()->new JuniperSigns.HangingWall(BlockBehaviour.Properties.copy(Blocks.OAK_WALL_HANGING_SIGN).dropsLike(JUNIPER_HANGING_SIGN.get())));
    public static final RegistryObject<net.minecraft.world.level.block.FlowerPotBlock> POTTED_JUNIPER_SAPLING=BLOCKS.register("potted_juniper_sapling",()->new net.minecraft.world.level.block.FlowerPotBlock(()->(net.minecraft.world.level.block.FlowerPotBlock)Blocks.FLOWER_POT,JUNIPER_SAPLING,BlockBehaviour.Properties.copy(Blocks.POTTED_OAK_SAPLING)));
    private static BlockBehaviour.Properties planks(){return BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(net.minecraft.world.level.material.MapColor.CLAY);}
    private static BlockBehaviour.Properties log(){return BlockBehaviour.Properties.copy(Blocks.OAK_LOG).mapColor(state->state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)==net.minecraft.core.Direction.Axis.Y?net.minecraft.world.level.material.MapColor.COLOR_CYAN:net.minecraft.world.level.material.MapColor.CLAY);}
    private PSBlocks() {}
    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().randomTicks().sound(SoundType.GRASS);
    }
}
