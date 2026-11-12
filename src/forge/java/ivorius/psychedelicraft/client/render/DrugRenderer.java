/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.client.render.shader.ColorPostEffect;
import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
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
            hallucinations = new HallucinationManager();
            COLOR_POST.releaseTarget();
        }
        if (player != null && world != null && !client.isGamePaused()) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
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
                float intensification = properties.getColorIntensification(hallucinations.strength(Effect.SUPER_SATURATION));
                float desaturation = properties.getDesaturation(hallucinations.strength(Effect.DESATURATION));
                COLOR_POST.render(client, player.ticksExisted + partialTicks, hallucinations, intensification, desaturation);
            }
        }
    }
}
