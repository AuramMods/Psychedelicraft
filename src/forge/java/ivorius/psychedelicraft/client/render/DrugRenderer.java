/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.client.render.shader.ColorPostEffect;
import ivorius.psychedelicraft.client.render.shader.TerrainShader;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class DrugRenderer {
    private static final ColorPostEffect COLOR_POST = new ColorPostEffect();
    private static final TerrainShader TERRAIN = new TerrainShader();
    private static WorldClient world;
    private static EntityPlayerSP player;
    private static HallucinationManager hallucinations = new HallucinationManager();
    private static EnvironmentEffects environment = new EnvironmentEffects();
    private static float pauseBlurProgress;

    private DrugRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft client = Minecraft.getMinecraft();
        if (client.world != world || client.player != player) {
            world = client.world;
            player = client.player;
            ContextualHallucinations.track(player, world);
            CameraEffects.reset(player);
            hallucinations = new HallucinationManager();
            environment = new EnvironmentEffects();
            pauseBlurProgress = 0F;
            COLOR_POST.releaseTarget();
        }
        pauseBlurProgress = MathHelper.clamp(pauseBlurProgress + (client.isGamePaused() ? 0.25F : -0.25F), 0F, 1F);
        if (player != null && world != null) environment.update(player);
        if (player != null && world != null && !client.isGamePaused()) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
                CameraEffects.tick(player, properties);
                ContextualHallucinations.tick(properties, hallucinations.strength(Effect.ENTITIES) * 0.4F);
                hallucinations.update(properties, player.getRNG(), player.ticksExisted);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        CameraEffects.transformView(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeHud(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            renderPost(Minecraft.getMinecraft(), event.getPartialTicks());
        }
    }

    public static void drawTerrain(ChunkRenderContainer chunks, BlockRenderLayer layer) {
        Minecraft client = Minecraft.getMinecraft();
        if (PSConfig.visual.shader3DEnabled && player != null && player == client.player && world == client.world) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null && TERRAIN.draw(client, player, hallucinations, properties, chunks, layer)) return;
        }
        chunks.renderChunkLayer(layer);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterWorld(RenderWorldLastEvent event) {
        Minecraft client = Minecraft.getMinecraft();
        ContextualHallucinations.render(client, event.getPartialTicks());
        if (PSConfig.visual.shader2DEnabled && player != null && player == client.player && world == client.world) {
            DrugProperties properties = DrugCapability.get(player);
            HarmoniumDrug harmonium = properties == null ? null : properties.getHarmonium();
            float peyote = properties == null ? 0F : (float) properties.getActiveValue(DrugType.PEYOTE);
            if (environment.heatDistortion() > 0F || environment.waterDistortion(peyote) > 0F
                    || (harmonium != null && harmonium.getActiveValue() > 0F)
                    || (properties != null && properties.getActiveValue(DrugType.ZERO) > 0F)
                    || hallucinations.strength(Effect.COLOR_CONTRAST) > 0F || ColorPostEffect.hasFocalBlur()) {
                COLOR_POST.captureDepth(client);
            }
        }
        // F1 suppresses the HUD event, so use the final world pass in that case.
        if (client.gameSettings.hideGUI && client.currentScreen == null
                && (!client.gameSettings.anaglyph || EntityRenderer.anaglyphField == 1)) {
            renderPost(client, event.getPartialTicks());
        }
    }

    private static void renderPost(Minecraft client, float partialTicks) {
        if (player != null && player == client.player && world == client.world) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
                PowerOverlay.render(client, player.ticksExisted, partialTicks, (float) properties.getActiveValue(DrugType.POWER));
                float pauseBlur = PSConfig.visual.pauseMenuBlur * pauseBlurProgress * pauseBlurProgress * pauseBlurProgress;
                COLOR_POST.render(client, partialTicks, hallucinations, properties, environment, pauseBlur);
            }
        }
    }
}
