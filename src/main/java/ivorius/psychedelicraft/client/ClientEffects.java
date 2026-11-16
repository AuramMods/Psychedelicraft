package ivorius.psychedelicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugNetwork.Snapshot;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
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
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Dist.CLIENT)
public final class ClientEffects {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Snapshot> pending = new HashMap<>();
    private static PostChain colors;
    private static PostPass colorPass;
    private static boolean failed;
    private static int width, height;

    private ClientEffects() {}

    public static void receive(Snapshot message) {
        pending.put(message.player(), message);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
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
        release();
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
    public static void world(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        DrugProperties properties = DrugCapability.get(client.player);
        float rotation = properties.getModifier(Drug::colorHallucinationStrength);
        if (rotation <= 0 || failed) return;
        try {
            prepare(client);
            colorPass.getEffect().safeGetUniform("ticks").set(client.player.tickCount + event.getPartialTick());
            colorPass.getEffect().safeGetUniform("slowColorRotation").set(rotation);
            RenderSystem.backupProjectionMatrix();
            try {
                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.resetTextureMatrix();
                colors.process(event.getPartialTick());
            } finally {
                RenderSystem.restoreProjectionMatrix();
                client.getMainRenderTarget().bindWrite(true);
                RenderSystem.enableDepthTest();
                RenderSystem.defaultBlendFunc();
            }
        } catch (IOException error) {
            failed = true;
            LOGGER.error("Could not load Psychedelicraft color shader", error);
        }
    }

    private static void prepare(Minecraft client) throws IOException {
        if (colors == null) {
            colors = new PostChain(client.getTextureManager(), client.getResourceManager(),
                    client.getMainRenderTarget(), Psychedelicraft.id("shaders/post/simple_effects.json"));
            colors.addTempTarget("swap", client.getWindow().getWidth(), client.getWindow().getHeight());
            colorPass = colors.addPass("psychedelicraft:simple_effects", client.getMainRenderTarget(), colors.getTempTarget("swap"));
            colors.addPass("blit", colors.getTempTarget("swap"), client.getMainRenderTarget());
        }
        int currentWidth = client.getWindow().getWidth();
        int currentHeight = client.getWindow().getHeight();
        if (currentWidth != width || currentHeight != height) {
            colors.resize(currentWidth, currentHeight);
            width = currentWidth;
            height = currentHeight;
        }
    }

    private static void release() {
        if (colors != null) colors.close();
        colors = null;
        colorPass = null;
        width = height = 0;
        failed = false;
    }

    @Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Resources {
        private Resources() {}

        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    (ResourceManager manager) -> release());
        }
    }
}
