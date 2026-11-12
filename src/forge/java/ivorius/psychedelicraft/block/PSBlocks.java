package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.TallCropBlock.Crop;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.LatticeCropBlockEntity;
import ivorius.psychedelicraft.block.entity.PeyoteBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSBlocks {
    public static final TallCropBlock CANNABIS = named("cannabis", new TallCropBlock(Crop.CANNABIS, false));
    // All four metadata bits of cultivated cannabis are needed for its 16 ages.
    public static final TallCropBlock NATURAL_CANNABIS = named("natural_cannabis", new TallCropBlock(Crop.CANNABIS, true));
    public static final TallCropBlock HOP = named("hop", new TallCropBlock(Crop.HOP, false));
    public static final TallCropBlock COCA = named("coca", new TallCropBlock.Coca());
    public static final TallCropBlock TOBACCO = named("tobacco", new TallCropBlock.TwoPart(Crop.TOBACCO));
    public static final TallCropBlock COFFEA = named("coffea", new TallCropBlock.TwoPart(Crop.COFFEA));
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
    public static final DryingTableBlock DRYING_TABLE = named("drying_table", new DryingTableBlock(Material.WOOD));
    public static final DryingTableBlock IRON_DRYING_TABLE = named("iron_drying_table", new DryingTableBlock(Material.IRON));

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
                LATTICE, MORNING_GLORY, MORNING_GLORY_LATTICE, WINE_GRAPE_LATTICE);
        GameRegistry.registerTileEntity(DryingTableBlockEntity.class, Psychedelicraft.id("drying_table"));
        GameRegistry.registerTileEntity(LatticeCropBlockEntity.class, Psychedelicraft.id("lattice_crop"));
        GameRegistry.registerTileEntity(PeyoteBlockEntity.class, Psychedelicraft.id("peyote"));
        Blocks.FIRE.setFireInfo(DRYING_TABLE, 5, 20);
        Blocks.FIRE.setFireInfo(LATTICE, 5, 20);
        Blocks.FIRE.setFireInfo(MORNING_GLORY_LATTICE, 5, 20);
        Blocks.FIRE.setFireInfo(WINE_GRAPE_LATTICE, 5, 20);
    }
}
