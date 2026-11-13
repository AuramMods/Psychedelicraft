package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PSConfigGuiFactory extends DefaultGuiFactory {
    public PSConfigGuiFactory() {
        super(Psychedelicraft.MOD_ID, "Psychedelicraft");
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        List<IConfigElement> root = ConfigElement.from(PSConfig.class).getChildElements();
        List<IConfigElement> elements = new ArrayList<>();
        IConfigElement visual = named(root, "visual");
        elements.add(section("visuals", Arrays.asList(
                section("shaders", options(visual, "shader2DEnabled", "shader3DEnabled", "doHeatDistortion",
                        "doWaterDistortion", "doMotionBlur", "pauseMenuBlur")),
                section("overlays", options(visual, "waterOverlayEnabled", "hurtOverlayEnabled", "sunFlareIntensity")),
                section("dof", Arrays.asList(
                        new DummyCategoryElement("near", "gui.psychedelicraft.option.focal_point.near",
                                options(visual, "dofFocalPointNear", "dofFocalBlurNear")),
                        new DummyCategoryElement("far", "gui.psychedelicraft.option.focal_point.far",
                                options(visual, "dofFocalPointFar", "dofFocalBlurFar")))),
                section("digital", options(visual, "digitalEffectPixelRescaleX", "digitalEffectPixelRescaleY")))));
        elements.add(ClientMusicConfig.configElement());
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null || client.isIntegratedServerRunning()) {
            IConfigElement balancing = named(root, "balancing");
            IConfigElement disabledMolotovs = named(balancing.getChildElements(), "disableMolotovs");
            IConfigElement riftInterval = named(balancing.getChildElements(), "randomTicksUntilRiftSpawn");
            elements.add(section("gameplay", Arrays.asList(
                    named(balancing.getChildElements(), "messagedistortion"),
                    section("features", Arrays.asList(named(balancing.getChildElements(), "enableHarmonium"),
                            named(balancing.getChildElements(), "enableRiftJars"),
                            new ConvertedOption(disabledMolotovs, "molotovs", ConfigGuiType.BOOLEAN, null, null,
                                    value -> !Boolean.parseBoolean(value.toString()),
                                    value -> !Boolean.parseBoolean(value.toString())))),
                    section("balancing", Arrays.asList(
                            new ConvertedOption(riftInterval, "rift_spawnrate", ConfigGuiType.INTEGER, 0, 1800,
                                    value -> Integer.parseInt(value.toString()) / PSConfig.MINUTE,
                                    value -> Integer.parseInt(value.toString()) * PSConfig.MINUTE),
                            named(balancing.getChildElements(), "dryingTableTickDuration"),
                            named(balancing.getChildElements(), "ironDryingTableTickDuration"),
                            named(balancing.getChildElements(), "slurryHardeningTime"),
                            named(balancing.getChildElements(), "fluidattributes"))),
                    named(balancing.getChildElements(), "worldgeneration"))));
        }
        return new GuiConfig(parent, elements, Psychedelicraft.MOD_ID, false, false,
                I18n.format("gui.psychedelicraft.options.title"));
    }

    private static IConfigElement section(String name, List<IConfigElement> children) {
        return new DummyCategoryElement(name, "gui.psychedelicraft.options." + name, children);
    }

    private static List<IConfigElement> options(IConfigElement category, String... names) {
        List<IConfigElement> children = category.getChildElements();
        List<IConfigElement> result = new ArrayList<>();
        for (String name : names) result.add(named(children, name));
        return result;
    }

    private static IConfigElement named(List<IConfigElement> elements, String name) {
        return elements.stream().filter(element -> element.getName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing Forge configuration entry " + name));
    }

    private static final class ConvertedOption extends DummyConfigElement {
        private final IConfigElement property;
        private final Function<Object, Object> toDisplay;
        private final Function<Object, Object> toStored;

        private ConvertedOption(IConfigElement property, String name, ConfigGuiType type, Integer min, Integer max,
                                Function<Object, Object> toDisplay, Function<Object, Object> toStored) {
            super(property.getName(), toDisplay.apply(property.getDefault()), type,
                    "gui.psychedelicraft.option.gameplay." + name, min, max);
            this.property = property;
            this.toDisplay = toDisplay;
            this.toStored = toStored;
        }

        @Override
        public Object get() {
            return toDisplay.apply(property.get());
        }

        @Override
        public void set(Object value) {
            property.set(toStored.apply(value));
        }

        @Override
        public boolean hasSlidingControl() {
            return getType() == ConfigGuiType.INTEGER;
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Psychedelicraft.MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(Psychedelicraft.MOD_ID, Config.Type.INSTANCE);
            ClientMusicConfig.save();
        }
    }
}
