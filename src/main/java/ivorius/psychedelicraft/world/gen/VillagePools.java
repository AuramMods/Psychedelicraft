package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class VillagePools {
    public static final List<String> PATHS=List.of("village/plains/houses","village/desert/houses","village/desert/decor");
    private VillagePools(){}
    @SubscribeEvent public static void beforeWorldGeneration(ServerAboutToStartEvent event){install(event.getServer().registryAccess());}
    public static void install(RegistryAccess registries){
        var pools=registries.registryOrThrow(Registries.TEMPLATE_POOL);
        for(String path:PATHS){
            var sourceId=new ResourceLocation("psychedelicraftmc",path);
            var source=pools.get(sourceId);
            var target=pools.get(new ResourceLocation("minecraft",path));
            // Datapacks may deliberately remove a pool. Leave their remaining village graph intact.
            if(source!=null&&target!=null)((VillagePoolExtension)target).psychedelicraft$append(sourceId,source);
        }
    }
}
