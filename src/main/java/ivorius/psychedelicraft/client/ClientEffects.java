package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.CameraEffects;
import ivorius.psychedelicraft.client.render.ScreenOverlays;
import ivorius.psychedelicraft.client.render.ContextualHallucinations;
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
    private static ScreenOverlays overlays=new ScreenOverlays();
    private static org.joml.Matrix4f worldProjection;
    private static net.minecraft.client.multiplayer.ClientLevel visualLevel;
    private static Player visualPlayer;
    private static boolean visualAlive;
    private static float pauseProgress;

    private ClientEffects() {}

    public static HallucinationManager hallucinations(){return hallucinations;}
    public static CameraEffects camera(){return camera;}

    public static void captureOculusHandMasks(Object targets) {
        post.captureOculusHandMasks(targets);
    }

    public static void captureBeforeOculusTranslucentHand() {
        var client = Minecraft.getInstance();
        if (client.level != null && ClientDrugState.alive(client.player)) {
            post.captureWorldDepth(client, hallucinations, environment);
        }
    }

    public static void observeDistantDepth(Object params, boolean solid) {
        var client = Minecraft.getInstance();
        if (ClientDrugState.alive(client.player)) post.observeDistantDepth(client, hallucinations, environment, params, solid);
    }

    public static void captureDistantOpaqueDepth(int texture, int width, int height) {
        post.captureDistantOpaqueDepth(texture, width, height);
    }

    public static void receive(Snapshot message) {
        pending.put(message.player(), message);
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.START){ivorius.psychedelicraft.client.render.RenderPhase.reset();worldProjection=null;post.beginFrame();}}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        boolean alive=ClientDrugState.alive(client.player);
        ContextualHallucinations.track(alive?client.player:null,alive?client.level:null);
        if(client.level!=visualLevel||client.player!=visualPlayer||alive!=visualAlive){resetVisuals();visualLevel=client.level;visualPlayer=client.player;visualAlive=alive;}
        pauseProgress=Mth.clamp(pauseProgress+(client.isPaused()?.25F:-.25F),0,1);
        if(client.level==null||!alive){if(client.player!=null)pending.remove(client.player.getUUID());return;}
        if(!client.isPaused()){hallucinations.update(ClientDrugState.get(client.player),client.player.getRandom(),client.player.tickCount);environment.update(client.player);}
        Iterator<Snapshot> snapshots = pending.values().iterator();
        while (snapshots.hasNext()) {
            Snapshot snapshot = snapshots.next();
            if (!snapshot.dimension().equals(client.level.dimension().location())) continue;
            Player player = client.level.getPlayerByUUID(snapshot.player());
            if (player != null) {
                if(!ClientDrugState.alive(player)){snapshots.remove();continue;}
                var capability=player.getCapability(DrugCapability.TYPE);
                if(capability.isPresent()){capability.ifPresent(drugs->drugs.fromNbt(snapshot.data()));snapshots.remove();}
            }
        }
        camera.tick(client);
        if(!client.isPaused())overlays.tick(client,ClientDrugState.get(client.player));
        if(!client.isPaused())ContextualHallucinations.tick(ClientDrugState.get(client.player),hallucinations.strength(HallucinationManager.Effect.ENTITIES)*.4F);
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        pending.clear();
        ContextualHallucinations.track(null,null);
        resetVisuals();
        visualLevel=null;visualPlayer=null;visualAlive=false;
    }

    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void world(RenderLevelStageEvent event){
        if(!ClientDrugState.alive(Minecraft.getInstance().player))return;
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES)ContextualHallucinations.render(Minecraft.getInstance(),event.getPartialTick(),event.getPoseStack());
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var client=Minecraft.getInstance();if(client.player==null||client.level==null)return;
        worldProjection=new org.joml.Matrix4f(event.getProjectionMatrix());
        try(var phase=ivorius.psychedelicraft.client.render.RenderPhase.POST.enter()){post.captureWorldDepth(client,hallucinations,environment);}
    }

    /** GameRenderer's return is after hands; Forge's AFTER_LEVEL event is before them. */
    public static void completeWorld(float partial){
        var client=Minecraft.getInstance();if(!ClientDrugState.alive(client.player)||client.level==null||worldProjection==null)return;
        try(var phase=ivorius.psychedelicraft.client.render.RenderPhase.POST.enter()){
            overlays.render(client,ClientDrugState.get(client.player),environment,partial);
            post.render(client,partial,worldProjection,hallucinations,environment,pauseProgress);
        }finally{worldProjection=null;}
    }

    private static void resetVisuals(){post.release();hallucinations=new HallucinationManager();environment=new EnvironmentEffects();camera=new CameraEffects();overlays=new ScreenOverlays();worldProjection=null;pauseProgress=0;}

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
