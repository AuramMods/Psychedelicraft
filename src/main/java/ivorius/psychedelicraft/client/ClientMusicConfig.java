package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;

final class ClientMusicConfig {
    private static final String CATEGORY = "background_music";
    private static final Configuration CONFIG = new Configuration(
            new File(Loader.instance().getConfigDir(), "psychedelicraft-client.cfg"));
    private static final Map<DrugType, Property> THEMES = new IdentityHashMap<>();

    static {
        CONFIG.load();
        CONFIG.getCategory(CATEGORY).setLanguageKey("gui.psychedelicraft.options.themes");
        for (DrugType type : DrugType.values()) {
            String path = type.id().substring(type.id().indexOf(':') + 1);
            THEMES.put(type, CONFIG.get(CATEGORY, path, true)
                    .setLanguageKey("gui.psychedelicraft.theme." + path));
        }
        save();
    }

    private ClientMusicConfig() {
    }

    static boolean enabled(DrugType type) {
        return THEMES.get(type).getBoolean();
    }

    static IConfigElement configElement() {
        return new ConfigElement(CONFIG.getCategory(CATEGORY));
    }

    static void save() {
        if (CONFIG.hasChanged()) CONFIG.save();
    }
}
