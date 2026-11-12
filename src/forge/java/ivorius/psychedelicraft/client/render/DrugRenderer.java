/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.client.render.shader.ColorPostEffect;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Material;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.IMultipassModel;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class DrugRenderer {
    private static final ColorPostEffect COLOR_POST = new ColorPostEffect();
    private static final WorldGeometryShader GEOMETRY = new WorldGeometryShader();
    private static WorldClient world;
    private static EntityPlayerSP player;
    private static HallucinationManager hallucinations = new HallucinationManager();
    private static EnvironmentEffects environment = new EnvironmentEffects();
    private static float pauseBlurProgress;
    private static boolean renderingWorld;
    private static boolean renderingBuiltInItem;
    @Nullable
    private static BlockPos blockItemAnchor;

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
        try (Scope scope = beginGeometry(Minecraft.getMinecraft(), Material.TERRAIN, 0F)) {
            chunks.renderChunkLayer(layer);
        }
    }

    public static void drawParticles(Tessellator tessellator) {
        try (Scope scope = beginGeometry(Minecraft.getMinecraft(), Material.PARTICLES, 0F)) {
            tessellator.draw();
        }
    }

    public static void renderWorld(EntityRenderer renderer, float partialTicks, long finishTime) {
        renderingWorld = true;
        try {
            renderer.renderWorld(partialTicks, finishTime);
        } finally {
            renderingWorld = false;
            GEOMETRY.clearEntityView();
        }
    }

    public static void captureEntityView() {
        if (renderingWorld && PSConfig.visual.shader3DEnabled && player != null) GEOMETRY.captureEntityView();
    }

    public static void drawEntityModel(ModelBase model, Entity entity, float limbSwing, float swingAmount,
                                       float age, float headYaw, float headPitch, float scale, boolean glint) {
        try (Scope scope = beginWorldModel(glint ? Material.ENTITY_GLINT : Material.ENTITY)) {
            model.render(entity, limbSwing, swingAmount, age, headYaw, headPitch, scale);
        }
    }

    public static void drawBoatWaterMask(IMultipassModel model, Entity entity, float limbSwing, float swingAmount,
                                        float age, float headYaw, float headPitch, float scale) {
        try (Scope scope = beginWorldModel()) {
            model.renderMultipass(entity, limbSwing, swingAmount, age, headYaw, headPitch, scale);
        }
    }

    public static void renderFirstPersonItems(ItemRenderer renderer, float partialTicks) {
        // The hand pass replaces the world model-view matrix before it renders its items and arms.
        captureEntityView();
        try {
            renderer.renderItemInFirstPerson(partialTicks);
        } finally {
            GEOMETRY.clearEntityView();
        }
    }

    public static void drawItemQuads(Tessellator tessellator, ItemStack stack) {
        // RenderItem supplies the empty stack for its two color-only glint draws.
        try (Scope scope = beginWorldModel(stack.isEmpty() ? Material.ENTITY_GLINT : Material.ENTITY, blockItemAnchor)) {
            tessellator.draw();
        }
    }

    public static void renderBlockItem(RenderItem renderer, ItemStack stack, BlockPos anchor) {
        BlockPos previous = blockItemAnchor;
        blockItemAnchor = anchor;
        try {
            renderer.renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        } finally {
            blockItemAnchor = previous;
        }
    }

    public static void drawMapQuads(Tessellator tessellator) {
        drawWorldQuads(tessellator);
    }

    public static void drawWorldQuads(Tessellator tessellator) {
        drawWorldQuads(tessellator, null);
    }

    public static void drawWorldQuads(Tessellator tessellator, @Nullable BlockPos anchor) {
        try (Scope scope = beginWorldModel(Material.ENTITY, anchor)) {
            tessellator.draw();
        }
    }

    public static void drawHandPart(ModelRenderer model, float scale) {
        try (Scope scope = beginWorldModel(Material.ENTITY)) {
            model.render(scale);
        }
    }

    public static void renderBuiltInItem(TileEntityItemStackRenderer renderer, ItemStack stack) {
        boolean previous = renderingBuiltInItem;
        renderingBuiltInItem = renderingWorld && renderer == TileEntityItemStackRenderer.instance;
        try {
            renderer.renderByItem(stack);
        } finally {
            renderingBuiltInItem = previous;
        }
    }

    public static void drawBuiltInModelList(int list) {
        try (Scope scope = renderingBuiltInItem ? beginWorldModel(Material.ENTITY, blockItemAnchor) : null) {
            GlStateManager.callList(list);
        }
    }

    @Nullable
    public static Scope beginWorldModel() {
        return beginWorldModel(Material.ENTITY);
    }

    public static Scope beginZeroMatter(ShaderManager program) {
        Minecraft client = Minecraft.getMinecraft();
        DrugProperties drugs = renderingWorld ? geometryDrugs(client) : null;
        return GEOMETRY.beginZeroMatter(program, client, player, hallucinations, drugs);
    }

    @Nullable
    private static Scope beginWorldModel(Material material) {
        return beginWorldModel(material, null);
    }

    @Nullable
    private static Scope beginWorldModel(Material material, @Nullable BlockPos anchor) {
        return renderingWorld ? beginGeometry(Minecraft.getMinecraft(), material, 0F, anchor) : null;
    }

    public static void drawFastClouds(Tessellator tessellator, double cameraY) {
        try (Scope scope = beginVanillaClouds(cameraY, false)) {
            tessellator.draw();
        }
    }

    @Nullable
    public static Scope beginVanillaClouds(double cameraY, boolean fancy) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null) return null;
        float height = client.world.provider.getCloudHeight() - (float) cameraY + 0.33F;
        return beginGeometry(client, fancy ? Material.VANILLA_FANCY_CLOUDS : Material.VANILLA_FAST_CLOUDS,
                MathHelper.floor(height / 4F) * 4F - height);
    }

    @Nullable
    public static Scope beginForgeClouds(float partialTicks, int mode) {
        Minecraft client = Minecraft.getMinecraft();
        Entity camera = client.getRenderViewEntity();
        if (client.world == null || camera == null) return null;
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
        double height = client.world.provider.getCloudHeight() - cameraY + 0.33D;
        return beginGeometry(client, mode == 2 ? Material.FORGE_FANCY_CLOUDS : Material.FORGE_FAST_CLOUDS,
                MathHelper.floor(height / 4D) * 4F);
    }

    public static void endClouds(@Nullable Scope scope) {
        if (scope != null) scope.close();
    }

    @Nullable
    private static Scope beginGeometry(Minecraft client, Material material, float cloudY) {
        return beginGeometry(client, material, cloudY, null);
    }

    @Nullable
    private static Scope beginGeometry(Minecraft client, Material material, float cloudY, @Nullable BlockPos anchor) {
        DrugProperties drugs = geometryDrugs(client);
        return drugs == null ? null : GEOMETRY.begin(client, player, hallucinations, drugs, material, cloudY, anchor);
    }

    @Nullable
    private static DrugProperties geometryDrugs(Minecraft client) {
        if (PSConfig.visual.shader3DEnabled && player != null && player == client.player && world == client.world) {
            return DrugCapability.get(player);
        }
        return null;
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
