package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.JuniperLeavesBlock;
import ivorius.psychedelicraft.block.LatticeCropBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.PeyoteBlockEntity;
import ivorius.psychedelicraft.client.render.block.DryingTableRenderer;
import ivorius.psychedelicraft.client.render.block.PeyoteRenderer;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PSItemModels {
    private PSItemModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : PSItems.items()) {
            if (item == PSItems.MORNING_GLORY_LATTICE || item == PSItems.WINE_GRAPE_LATTICE) {
                for (int age = 0; age <= LatticeCropBlock.MAX_AGE; age++) {
                    ModelLoader.setCustomModelResourceLocation(item, age, new ModelResourceLocation(
                            Psychedelicraft.id(item.getRegistryName().getResourcePath() + "_" + age), "inventory"));
                }
            } else {
                register(item);
            }
        }
        shareModel(PSBlocks.NATURAL_CANNABIS, PSBlocks.CANNABIS);
        shareModel(PSBlocks.NATURAL_HOP, PSBlocks.HOP);
        shareModel(PSBlocks.NATURAL_COCA, PSBlocks.COCA);
        shareModel(PSBlocks.NATURAL_TOBACCO, PSBlocks.TOBACCO);
        shareModel(PSBlocks.NATURAL_COFFEA, PSBlocks.COFFEA);
        shareModel(PSBlocks.POWERED_JUNIPER_TRAPDOOR, PSBlocks.JUNIPER_TRAPDOOR);
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_DOOR, new StateMap.Builder().ignore(BlockDoor.POWERED).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_FENCE_GATE, new StateMap.Builder().ignore(BlockFenceGate.POWERED).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_SLAB, new StateMap.Builder()
                .ignore(PSBlocks.JUNIPER_SLAB.getVariantProperty()).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_DOUBLE_SLAB, new StateMap.Builder()
                .ignore(PSBlocks.JUNIPER_DOUBLE_SLAB.getVariantProperty()).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_LEAVES, new StateMap.Builder()
                .ignore(JuniperLeavesBlock.DISTANCE, JuniperLeavesBlock.PERSISTENT).build());
        ModelLoader.setCustomStateMapper(PSBlocks.FRUITING_JUNIPER_LEAVES, new StateMap.Builder()
                .ignore(JuniperLeavesBlock.DISTANCE, JuniperLeavesBlock.PERSISTENT).build());
        ClientRegistry.bindTileEntitySpecialRenderer(DryingTableBlockEntity.class, new DryingTableRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(PeyoteBlockEntity.class, new PeyoteRenderer());
    }

    private static void shareModel(Block block, Block model) {
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(model.getRegistryName(), getPropertyString(state.getProperties()));
            }
        });
    }

    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
