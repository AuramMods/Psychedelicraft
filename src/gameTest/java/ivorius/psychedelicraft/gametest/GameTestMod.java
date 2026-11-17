package ivorius.psychedelicraft.gametest;

import net.minecraftforge.fml.common.Mod;

/** Development-only registration; excluded from the production mod jar. */
@Mod("psychedelicrafttests")
public final class GameTestMod {
    public GameTestMod(){
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerAboutToStartEvent event)->{
            // Tests change real config values synchronously; the disk watcher must not replay older autosaves between assertions.
            for(var config:net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.COMMON))
                if(config.getModId().equals(ivorius.psychedelicraft.Psychedelicraft.MOD_ID))
                    com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().removeWatch(config.getFullPath());
        });
    }
}
