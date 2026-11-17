package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.CameraEffects;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugNetwork.Snapshot;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.client.Minecraft;
import ivorius.psychedelicraft.client.render.HallucinationManager;
import ivorius.psychedelicraft.client.render.EnvironmentEffects;
import ivorius.psychedelicraft.client.render.shader.PostEffects;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Dist.CLIENT)
public final class ClientEffects {
    private static final Map<UUID, Snapshot> pending = new HashMap<>();
    private static final PostEffects post=new PostEffects();
    private static HallucinationManager hallucinations=new HallucinationManager();
    private static EnvironmentEffects environment=new EnvironmentEffects();
    private static CameraEffects camera=new CameraEffects();
    private static net.minecraft.client.multiplayer.ClientLevel visualLevel;
    private static Player visualPlayer;
    private static float pauseProgress;

    private ClientEffects() {}

    public static HallucinationManager hallucinations(){return hallucinations;}
    public static CameraEffects camera(){return camera;}

    public static void receive(Snapshot message) {
        pending.put(message.player(), message);
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.START)ivorius.psychedelicraft.client.render.RenderPhase.reset();}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if(client.level!=visualLevel||client.player!=visualPlayer){resetVisuals();visualLevel=client.level;visualPlayer=client.player;}
        pauseProgress=Mth.clamp(pauseProgress+(client.isPaused()?.25F:-.25F),0,1);
        if(client.level==null||client.player==null)return;
        if(!client.isPaused()){hallucinations.update(DrugCapability.get(client.player),client.player.getRandom(),client.player.tickCount);environment.update(client.player);}
        Iterator<Snapshot> snapshots = pending.values().iterator();
        while (snapshots.hasNext()) {
            Snapshot snapshot = snapshots.next();
            if (!snapshot.dimension().equals(client.level.dimension().location())) continue;
            Player player = client.level.getPlayerByUUID(snapshot.player());
            if (player != null) {
                DrugCapability.get(player).fromNbt(snapshot.data());
                snapshots.remove();
            }
        }
        camera.tick(client);
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        pending.clear();
        resetVisuals();
        visualLevel=null;visualPlayer=null;
    }

    @SubscribeEvent
    public static void world(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var client=Minecraft.getInstance();if(client.player==null||client.level==null)return;
        try(var phase=ivorius.psychedelicraft.client.render.RenderPhase.POST.enter()){post.render(client,event.getPartialTick(),event.getProjectionMatrix(),hallucinations,environment,pauseProgress);}
    }

    private static void resetVisuals(){post.release();hallucinations=new HallucinationManager();environment=new EnvironmentEffects();camera=new CameraEffects();pauseProgress=0;}

    @Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Resources {
        private Resources() {}

        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    (ResourceManager manager) -> post.release());
        }
    }
}
