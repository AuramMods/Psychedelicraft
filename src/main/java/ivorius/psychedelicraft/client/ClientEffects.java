package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.Drug;
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
import net.minecraftforge.client.event.ViewportEvent;
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
    private static net.minecraft.client.multiplayer.ClientLevel visualLevel;
    private static Player visualPlayer;
    private static float pauseProgress;

    private ClientEffects() {}

    public static void receive(Snapshot message) {
        pending.put(message.player(), message);
    }

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
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        pending.clear();
        resetVisuals();
        visualLevel=null;visualPlayer=null;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || event.getCamera().getEntity() != client.player) return;
        float wobble = DrugCapability.get(client.player).getModifier(Drug::viewWobblyness);
        float ticks = client.player.tickCount + (float) event.getPartialTick();
        event.setRoll(event.getRoll() + Mth.sin(ticks * 0.07F) * wobble * Mth.RAD_TO_DEG);
    }

    @SubscribeEvent
    public static void world(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var client=Minecraft.getInstance();if(client.player==null||client.level==null)return;
        post.render(client,event.getPartialTick(),event.getProjectionMatrix(),hallucinations,environment,pauseProgress);
    }

    private static void resetVisuals(){post.release();hallucinations=new HallucinationManager();environment=new EnvironmentEffects();pauseProgress=0;}

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
