package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PSConfigGuiFactory extends DefaultGuiFactory {
    public PSConfigGuiFactory() {
        super(Psychedelicraft.MOD_ID, "Psychedelicraft");
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        List<IConfigElement> elements = new ArrayList<>(ConfigElement.from(PSConfig.class).getChildElements());
        elements.add(ClientMusicConfig.configElement());
        return new GuiConfig(parent, elements, Psychedelicraft.MOD_ID, false, false, title);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Psychedelicraft.MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(Psychedelicraft.MOD_ID, Config.Type.INSTANCE);
            ClientMusicConfig.save();
        }
    }
}
