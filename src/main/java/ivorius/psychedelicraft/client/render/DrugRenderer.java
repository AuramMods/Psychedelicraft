/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.RiftJarBlock;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.IMultipassModel;
import net.minecraft.client.model.ModelBanner;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBed;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class DrugRenderer {
    private static final ColorPostEffect COLOR_POST = new ColorPostEffect();
    private static final WorldGeometryShader GEOMETRY = new WorldGeometryShader();
    private static WorldClient world;
    private static EntityPlayerSP player;
    private static HallucinationManager hallucinations = new HallucinationManager();
    private static EnvironmentEffects environment = new EnvironmentEffects();
    private static ScreenOverlays screenOverlays = new ScreenOverlays();
    private static float pauseBlurProgress;
    private static boolean renderingWorld;
    private static boolean renderingBuiltInItem;
    @Nullable
    private static BlockPos blockItemAnchor;
    @Nullable
    private static BlockPos shadowSection;

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
            screenOverlays = new ScreenOverlays();
            pauseBlurProgress = 0F;
            COLOR_POST.releaseTarget();
        }
        pauseBlurProgress = MathHelper.clamp(pauseBlurProgress + (client.isGamePaused() ? 0.25F : -0.25F), 0F, 1F);
        if (player != null && world != null) environment.update(player);
        if (player != null && world != null && !client.isGamePaused()) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
                CameraEffects.tick(player, properties);
                screenOverlays.tick(client, player, properties);
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

    public static void drawBlockCracks(BlockRendererDispatcher renderer, IBlockState state, BlockPos pos,
                                       TextureAtlasSprite crack, IBlockAccess access, Tessellator tessellator) {
        try (Scope scope = beginGeometry(Minecraft.getMinecraft(), Material.BLOCK_DAMAGE, 0F, pos)) {
            renderer.renderBlockDamage(state, pos, crack, access);
            if (scope != null && tessellator.getBuffer().getVertexCount() > 0) {
                // The damage batch uses camera-relative vertices. Draw with this block's section offset.
                tessellator.draw();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                buffer.noColor();
            }
        }
    }

    public static void drawBlockOutline(AxisAlignedBB box, float red, float green, float blue, float alpha,
                                        RayTraceResult target) {
        Minecraft client = Minecraft.getMinecraft();
        BlockPos pos = target.getBlockPos();
        try (Scope scope = beginGeometry(client, Material.BLOCK_OUTLINE, 0F, pos)) {
            if (client.world != null && client.world.getBlockState(pos).getBlock() == PSBlocks.RIFT_JAR) {
                // Vanilla passes the camera-relative enclosing box, padded equally on all sides.
                Vec3d origin = box.getCenter().subtract(PSBlocks.RIFT_JAR.getDefaultState()
                        .getBoundingBox(client.world, pos).getCenter());
                for (AxisAlignedBB part : RiftJarBlock.SHAPE) {
                    RenderGlobal.drawSelectionBoundingBox(part.offset(origin.x, origin.y, origin.z), red, green, blue, alpha);
                }
            } else {
                RenderGlobal.drawSelectionBoundingBox(box, red, green, blue, alpha);
            }
        }
    }

    public static void drawWorldBorder(Tessellator tessellator) {
        try (Scope scope = beginGeometry(Minecraft.getMinecraft(), Material.WORLD_BORDER, 0F)) {
            tessellator.draw();
        }
    }

    public static void drawParticles(Tessellator tessellator) {
        try (Scope scope = beginGeometry(Minecraft.getMinecraft(), Material.PARTICLES, 0F)) {
            tessellator.draw();
        }
    }

    public static void renderSky(RenderGlobal renderer, float partialTicks, int pass) {
        Minecraft client = Minecraft.getMinecraft();
        if (geometryDrugs(client) == null || (hallucinations.strength(Effect.SURFACE_FRACTALS) <= 0F
                && hallucinations.strength(Effect.PULSES) <= 0F)) {
            renderer.renderSky(partialTicks, pass);
            return;
        }
        // Recover the coordinates after each sky draw's local rotation, before the camera transforms them.
        GEOMETRY.captureEntityView();
        try {
            renderer.renderSky(partialTicks, pass);
        } finally {
            GEOMETRY.clearEntityView();
        }
    }

    public static void drawSky(Tessellator tessellator, boolean textured) {
        try (Scope scope = GEOMETRY.beginSky(Minecraft.getMinecraft(), hallucinations, textured)) {
            tessellator.draw();
        }
    }

    public static void drawCelestial(Tessellator tessellator) {
        try (Scope scope = beginSkyMesh(Material.CELESTIAL)) {
            tessellator.draw();
        }
    }

    public static void drawSkyBuffer(VertexBuffer buffer, int mode, boolean stars) {
        try (Scope scope = beginSkyMesh(stars ? Material.STARS : Material.SKY_PLANES)) {
            buffer.drawArrays(mode);
        }
    }

    public static void drawSkyList(int list, boolean stars) {
        try (Scope scope = beginSkyMesh(stars ? Material.STARS : Material.SKY_PLANES)) {
            GlStateManager.callList(list);
        }
    }

    private static Scope beginSkyMesh(Material material) {
        return GEOMETRY.beginSky(Minecraft.getMinecraft(), hallucinations, material);
    }

    public static void renderWorld(EntityRenderer renderer, float partialTicks, long finishTime) {
        renderingWorld = true;
        try {
            renderer.renderWorld(partialTicks, finishTime);
        } finally {
            renderingWorld = false;
            shadowSection = null;
            GEOMETRY.clearEntityView();
        }
        Minecraft client = Minecraft.getMinecraft();
        // F1 skips the HUD, so composite once after the complete world and hand render.
        if (client.gameSettings.hideGUI && client.currentScreen == null) renderPost(client, partialTicks);
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
        drawWorldQuads(tessellator, Material.ENTITY, anchor);
    }

    private static void drawWorldQuads(Tessellator tessellator, Material material, @Nullable BlockPos anchor) {
        try (Scope scope = beginWorldModel(material, anchor)) {
            tessellator.draw();
        }
    }

    public static void drawHandPart(ModelRenderer model, float scale) {
        drawWorldPart(model, scale);
    }

    public static void drawWorldPart(ModelRenderer model, float scale) {
        try (Scope scope = beginWorldModel(Material.ENTITY)) {
            model.render(scale);
        }
    }

    public static void drawBanner(ModelBanner model) {
        try (Scope scope = beginTileModel()) {
            model.renderBanner();
        }
    }

    public static void drawBed(ModelBed model) {
        try (Scope scope = beginTileModel()) {
            model.render();
        }
    }

    public static void drawChest(ModelChest model) {
        try (Scope scope = beginTileModel()) {
            model.renderAll();
        }
    }

    public static void drawSign(ModelSign model) {
        try (Scope scope = beginTileModel()) {
            model.renderSign();
        }
    }

    public static void drawTileModel(ModelBase model, Entity entity, float limbSwing, float swingAmount,
                                     float age, float headYaw, float headPitch, float scale) {
        try (Scope scope = beginTileModel()) {
            model.render(entity, limbSwing, swingAmount, age, headYaw, headPitch, scale);
        }
    }

    public static void drawTilePart(ModelRenderer model, float scale) {
        try (Scope scope = beginTileModel()) {
            model.render(scale);
        }
    }

    public static int drawSignText(FontRenderer font, String text, int x, int y, int color) {
        try (Scope scope = beginTileModel()) {
            return font.drawString(text, x, y, color);
        }
    }

    public static void drawNameplateBackground(Tessellator tessellator) {
        Material material = GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? Material.ENTITY : Material.ENTITY_UNIFORM_PULSE;
        drawWorldQuads(tessellator, material, null);
    }

    public static int drawNameplateText(FontRenderer font, String text, int x, int y, int color, boolean seeThrough) {
        try (Scope scope = beginWorldModel(seeThrough ? Material.ENTITY_UNIFORM_PULSE : Material.ENTITY)) {
            return font.drawString(text, x, y, color);
        }
    }

    public static void drawPiston(Tessellator tessellator, TileEntityPiston piston) {
        // The translucent moving-block shader has no vertex distance. Solid and cutout blocks use terrain programs.
        boolean translucent = piston.getPistonState().getBlock().getBlockLayer() == BlockRenderLayer.TRANSLUCENT;
        drawWorldQuads(tessellator, translucent ? Material.ENTITY_UNIFORM_PULSE : Material.ENTITY, piston.getPos());
    }

    public static void drawBeaconBeam(Tessellator tessellator) {
        // End gateways use the same beacon renderer for their beams.
        drawWorldQuads(tessellator, Material.ENTITY_UNIFORM_PULSE, null);
    }

    public static void drawPortalLayer(Tessellator tessellator) {
        try (Scope scope = beginWorldModel(Material.END_PORTAL)) {
            tessellator.draw();
        }
    }

    @Nullable
    private static Scope beginTileModel() {
        // Built-in items already shade their display lists with the parent item's coordinates.
        return renderingBuiltInItem ? null : beginWorldModel();
    }

    static void drawScreenQuads(Tessellator tessellator) {
        Minecraft client = Minecraft.getMinecraft();
        try (Scope scope = PSConfig.visual.shader3DEnabled && player != null
                ? GEOMETRY.beginScreen(client, player, hallucinations) : null) {
            tessellator.draw();
        }
    }

    public static void drawWorldBlock(BlockRendererDispatcher renderer, IBlockState state, float brightness) {
        try (Scope scope = beginWorldModel()) {
            renderer.renderBlockBrightness(state, brightness);
        }
    }

    public static void shadowSurface(BlockPos surface) {
        if (!renderingWorld || !PSConfig.visual.shader3DEnabled) return;
        // Vanilla passes the air position above the receiving block.
        int x = surface.getX() & ~15;
        int y = (surface.getY() - 1) & ~15;
        int z = surface.getZ() & ~15;
        if (shadowSection != null) {
            if (shadowSection.getX() == x && shadowSection.getY() == y && shadowSection.getZ() == z) return;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            if (buffer.getVertexCount() > 0) {
                try (Scope scope = beginWorldModel(Material.ENTITY, shadowSection)) {
                    if (scope != null) {
                        tessellator.draw();
                        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                    }
                }
            }
        }
        shadowSection = new BlockPos(x, y, z);
    }

    public static void drawShadow(Tessellator tessellator) {
        try {
            drawWorldQuads(tessellator, shadowSection);
        } finally {
            shadowSection = null;
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

    @Nullable
    public static Scope beginRiftCenter() {
        Minecraft client = Minecraft.getMinecraft();
        return GEOMETRY.begin(client, player, hallucinations, renderingWorld ? geometryDrugs(client) : null,
                Material.RIFT_CENTER, 0F, null);
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
    }

    private static void renderPost(Minecraft client, float partialTicks) {
        if (player != null && player == client.player && world == client.world) {
            DrugProperties properties = DrugCapability.get(player);
            if (properties != null) {
                float hurt = environment.hurtOverlay(player.hurtTime, player.maxHurtTime, properties.heartbeatPulse(partialTicks));
                screenOverlays.render(client, player, properties, partialTicks, hurt);
                float pauseBlur = PSConfig.visual.pauseMenuBlur * pauseBlurProgress * pauseBlurProgress * pauseBlurProgress;
                COLOR_POST.render(client, partialTicks, hallucinations, properties, environment, pauseBlur);
            }
        }
    }
}
