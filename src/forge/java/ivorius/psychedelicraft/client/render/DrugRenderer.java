/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.client.render.shader.ColorPostEffect;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
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
                ContextualHallucinations.tick(properties, hallucinations.strength(Effect.ENTITIES) * 0.4F);
                hallucinations.update(properties, player.getRNG(), player.ticksExisted);
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        Minecraft client = Minecraft.getMinecraft();
        DrugProperties properties = client.player == null ? null : DrugCapability.get(client.player);
        if (properties == null) {
            return;
        }

        float wobble = Math.min(1F, properties.getModifier(Drug::viewWobblyness));
        if (wobble <= 0) {
            return;
        }
        float tick = client.player.ticksExisted + (float) event.getRenderPartialTicks();
        float base = 5F / (wobble * wobble + 5F) - wobble * 0.04F;
        float square = base * base;
        float angle = tick * 3F;

        // The donor applies Ry * Rz, nonuniform scaling, then the inverse rotations.
        GlStateManager.rotate(angle, 0, 1, 0);
        GlStateManager.rotate(angle, 0, 0, 1);
        GlStateManager.scale(axisScale(tick, 150F, wobble, square),
                axisScale(tick, 170F, wobble, square), axisScale(tick, 190F, wobble, square));
        GlStateManager.rotate(-angle, 0, 0, 1);
        GlStateManager.rotate(-angle, 0, 1, 0);
    }

    private static float axisScale(float tick, float period, float wobble, float square) {
        return 1F / (square + wobble * MathHelper.sin(tick / period * (float) Math.PI) / 2F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeHud(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            renderPost(Minecraft.getMinecraft(), event.getPartialTicks());
        }
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
