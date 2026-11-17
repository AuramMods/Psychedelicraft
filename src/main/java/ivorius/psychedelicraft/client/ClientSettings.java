package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.screen.SettingsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSettings {
    private ClientSettings(){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        ModList.get().getModContainerById(Psychedelicraft.MOD_ID).orElseThrow().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,()->new ConfigScreenHandler.ConfigScreenFactory(SettingsScreen::new));
    }
}
