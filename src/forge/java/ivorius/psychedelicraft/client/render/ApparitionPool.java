package ivorius.psychedelicraft.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class ApparitionPool {
    private static final Logger LOG = LogManager.getLogger(Psychedelicraft.MOD_ID);
    private static final ResourceLocation PIG = new ResourceLocation("pig");
    private static List<ResourceLocation> single = Collections.singletonList(PIG);
    private static List<ResourceLocation> multiple = Collections.singletonList(PIG);

    private ApparitionPool() {
    }

    @SubscribeEvent
    public static void reload(ModelBakeEvent event) {
        IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
        single = load(resources, Psychedelicraft.id("single_entity_hallucinations"));
        multiple = load(resources, Psychedelicraft.id("multiple_entity_hallucinations"));
    }

    static ResourceLocation choose(boolean grouped, Random random) {
        List<ResourceLocation> choices = grouped ? multiple : single;
        return choices.isEmpty() ? PIG : choices.get(random.nextInt(choices.size()));
    }

    static ResourceLocation legacyId(ResourceLocation id) {
        if ("minecraft".equals(id.getResourceDomain())) {
            if ("snow_golem".equals(id.getResourcePath())) return new ResourceLocation("snowman");
            if ("iron_golem".equals(id.getResourcePath())) return new ResourceLocation("villager_golem");
        }
        return id;
    }

    private static List<ResourceLocation> load(IResourceManager resources, ResourceLocation pool) {
        Set<ResourceLocation> entries = read(resources, pool, new HashSet<>());
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : entries) {
            if (ApparitionModels.forId(id) != null || EntityList.getClass(legacyId(id)) != null) {
                result.add(id);
            } else {
                LOG.warn("No apparition renderer for {}", id);
            }
        }
        return result;
    }

    private static Set<ResourceLocation> read(IResourceManager resources, ResourceLocation pool, Set<ResourceLocation> path) {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        if (!path.add(pool)) {
            LOG.warn("Circular apparition pool reference {}", pool);
            return result;
        }
        ResourceLocation file = new ResourceLocation(pool.getResourceDomain(), "hallucinations/" + pool.getResourcePath() + ".json");
        try {
            for (IResource resource : resources.getAllResources(file)) {
                try (IResource entry = resource; Reader reader = new InputStreamReader(entry.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    if (json.has("replace") && json.get("replace").getAsBoolean()) result.clear();
                    for (JsonElement value : json.getAsJsonArray("values")) {
                        String id = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
                        if (id.startsWith("#")) result.addAll(read(resources, new ResourceLocation(id.substring(1)), path));
                        else result.add(new ResourceLocation(id));
                    }
                } catch (IOException | RuntimeException invalid) {
                    LOG.warn("Could not read apparition pool {}", file, invalid);
                }
            }
        } catch (IOException missing) {
            LOG.warn("No apparition pool resource {}", file);
        } finally {
            path.remove(pool);
        }
        return result;
    }
}
