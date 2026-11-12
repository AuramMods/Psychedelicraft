package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.TallCropBlock.Crop;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
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
                DRYING_TABLE, IRON_DRYING_TABLE);
        GameRegistry.registerTileEntity(DryingTableBlockEntity.class, Psychedelicraft.id("drying_table"));
        Blocks.FIRE.setFireInfo(DRYING_TABLE, 5, 20);
    }
}
