package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PSItemModels {
    private PSItemModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(PSItems.COFFEE_BEANS, 0,
                new ModelResourceLocation(Psychedelicraft.id("coffee_beans"), "inventory"));
    }
}
