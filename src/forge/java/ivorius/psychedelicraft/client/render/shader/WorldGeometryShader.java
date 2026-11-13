package ivorius.psychedelicraft.client.render.shader;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.NVFogDistance;

import javax.annotation.Nullable;
import java.io.IOException;

public final class WorldGeometryShader implements IResourceManagerReloadListener {
    private IResourceManager resources;
    private ShaderManager shader;
    private boolean failed;
    private final EntityRenderState entityState = new EntityRenderState();

    public void captureEntityView() {
        entityState.captureView();
    }

    public void clearEntityView() {
        entityState.clearView();
    }

    @Nullable
    public Scope begin(Minecraft client, EntityPlayerSP player, HallucinationManager hallucinations,
                       DrugProperties drugs, Material material, float cloudY, @Nullable BlockPos anchor) {
        return begin(client, player, hallucinations, drugs, material, cloudY, anchor, null);
    }

    public Scope beginZeroMatter(ShaderManager program, Minecraft client, @Nullable EntityPlayerSP player,
                                 HallucinationManager hallucinations, @Nullable DrugProperties drugs) {
        return begin(client, player, hallucinations, drugs, Material.ENTITY, 0F, null, program);
    }

    @Nullable
    public Scope beginScreen(Minecraft client, EntityPlayerSP player, HallucinationManager hallucinations) {
        return begin(client, player, hallucinations, null, Material.SCREEN, 0F, null, null);
    }

    @Nullable
    public Scope beginSky(Minecraft client, HallucinationManager hallucinations, boolean textured) {
        return begin(client, null, hallucinations, null, textured ? Material.END_SKY : Material.SKY_GRADIENT,
                0F, null, null);
    }

    @Nullable
    private Scope begin(Minecraft client, @Nullable EntityPlayerSP player, HallucinationManager hallucinations,
                        @Nullable DrugProperties drugs, Material material, float cloudY,
                        @Nullable BlockPos anchor, @Nullable ShaderManager zeroMatter) {
        boolean portal = material == Material.END_PORTAL;
        boolean entity = material == Material.ENTITY || material == Material.ENTITY_GLINT || portal;
        boolean screen = material == Material.SCREEN;
        boolean sky = material == Material.SKY_GRADIENT || material == Material.END_SKY;
        boolean deform = player != null && drugs != null && (!entity || entityState.hasView());
        if (!deform && !screen && (!sky || !entityState.hasView()) && zeroMatter == null) return null;
        float small = deform ? hallucinations.strength(Effect.SMALL_WAVES) * 0.5F : 0F;
        float big = deform ? hallucinations.strength(Effect.BIG_WAVES) * 0.6F : 0F;
        float wiggle = deform ? hallucinations.strength(Effect.WIGGLE_WAVES) * 0.7F : 0F;
        float distant = deform ? hallucinations.strength(Effect.DISTANT_WORLD_DEFORMATION) : 0F;
        // The donor's glint and End portal shaders do not use vertex color.
        float surface = (!deform && !screen && !sky) || material == Material.ENTITY_GLINT || portal ? 0F
                : MathHelper.clamp(hallucinations.strength(Effect.SURFACE_FRACTALS), 0F, 1F);
        float fracture = deform ? (float) drugs.getActiveValue(DrugType.LSD) * 0.06F : 0F;
        float[] pulses = deform || screen ? hallucinations.pulseColor() : null;

        ShaderManager program = zeroMatter;
        if (program == null) {
            if (small <= 0F && big <= 0F && wiggle <= 0F && distant <= 0F
                    && surface <= 0F && fracture <= 0F && pulses == null) return null;
            prepare(client);
            program = shader;
            if (program == null) return null;
        }

        int portalUnit = material == Material.TERRAIN ? 0 : entity ? 3 : 2;
        Scope scope = new Scope(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), GL13.GL_TEXTURE0 + portalUnit);
        try {
            // The caller owns the primary textures, alpha test, blend and depth passes.
            if (zeroMatter == null) {
                OpenGlHelper.glUseProgram(program.getProgram());
                setSampler(program, "ColorSampler", 0);
                setSampler(program, "LightmapSampler", 1);
                upload(program, "FogMode", screen || sky ? 0F : fogMode());
                upload(program, "RadialFog", !screen && !sky && radialFog() ? 1F : 0F);
                upload(program, "SkyMode", material == Material.SKY_GRADIENT ? 1F : sky ? 2F : 0F);
                upload(program, "PortalTexGen", portal ? 1F : 0F);
                if (entity) entityState.upload(program);
                else {
                    upload(program, "LightmapMode", material.lightmapMode);
                    if (sky) entityState.uploadView(program);
                }
            } else if (deform) {
                entityState.uploadView(program);
            }
            // Keep the inactive atlas sampler on the primary texture rather than an unused texture unit.
            setSampler(program, "PortalSampler", surface > 0F ? portalUnit : 0);
            upload(program, "WorldTicks", deform || screen ? player.ticksExisted + client.getRenderPartialTicks() : 0F);
            upload(program, "DistantDeformation", distant);
            upload(program, "SurfaceFractalStrength", surface);
            upload(program, "FractureStrength", fracture);
            upload(program, "EntityModel", entity && deform ? 1F : 0F);

            uploadCoordinates(program, material, cloudY, anchor);

            ShaderUniform waves = program.getShaderUniformOrDefault("Waves");
            waves.set(small, big, wiggle);
            waves.upload();
            ShaderUniform position = program.getShaderUniformOrDefault("PlayerXZ");
            position.set(deform ? (float) player.posX : 0F, deform ? (float) player.posZ : 0F);
            position.upload();
            ShaderUniform pulseColor = program.getShaderUniformOrDefault("Pulses");
            if (pulses == null) pulseColor.set(0F, 0F, 0F, 0F);
            else pulseColor.set(pulses);
            pulseColor.upload();
            if (surface > 0F) {
                if (material != Material.TERRAIN) scope.bindPortalAtlas(client);
                TextureAtlasSprite sprite = client.getBlockRendererDispatcher().getBlockModelShapes()
                        .getTexture(Blocks.PORTAL.getDefaultState());
                ShaderUniform coords = program.getShaderUniformOrDefault("SurfaceFractalCoords");
                coords.set(sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV());
                coords.upload();
            }
            return scope;
        } catch (RuntimeException | Error failure) {
            scope.close();
            throw failure;
        }
    }

    private static void setSampler(ShaderManager program, String name, int unit) {
        OpenGlHelper.glUniform1i(OpenGlHelper.glGetUniformLocation(program.getProgram(), name), unit);
    }

    private static void upload(ShaderManager program, String name, float value) {
        ShaderUniform uniform = program.getShaderUniformOrDefault(name);
        uniform.set(value);
        uniform.upload();
    }

    private static void uploadCoordinates(ShaderManager program, Material material, float cloudY, @Nullable BlockPos anchor) {
        ShaderUniform coordinates = program.getShaderUniformOrDefault("GeometryCoordinates");
        if (anchor == null) {
            coordinates.set(0F, cloudY, 0F, material.horizontalScale);
        } else {
            // A chunk buffer stores vertices relative to its section origin. Use that same origin for attached details.
            coordinates.set((float) (TileEntityRendererDispatcher.staticPlayerX - (anchor.getX() & ~15)),
                    (float) (TileEntityRendererDispatcher.staticPlayerY - (anchor.getY() & ~15)),
                    (float) (TileEntityRendererDispatcher.staticPlayerZ - (anchor.getZ() & ~15)), material.horizontalScale);
        }
        coordinates.upload();
    }

    private static float fogMode() {
        if (!GL11.glIsEnabled(GL11.GL_FOG)) return 0F;
        switch (GL11.glGetInteger(GL11.GL_FOG_MODE)) {
            case GL11.GL_LINEAR: return 1F;
            case GL11.GL_EXP: return 2F;
            case GL11.GL_EXP2: return 3F;
            default: return 0F;
        }
    }

    private static boolean radialFog() {
        return GLContext.getCapabilities().GL_NV_fog_distance
                && GL11.glGetInteger(NVFogDistance.GL_FOG_DISTANCE_MODE_NV) == NVFogDistance.GL_EYE_RADIAL_NV;
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        if (shader != null) shader.deleteShader();
        resources = manager;
        shader = null;
        failed = false;
    }

    private void prepare(Minecraft client) {
        if (!OpenGlHelper.shadersSupported) return;
        if (resources != client.getResourceManager()) {
            ((IReloadableResourceManager) client.getResourceManager()).registerReloadListener(this);
        }
        if (shader != null || failed) return;
        try {
            ShaderManager program = new ShaderManager(resources, Psychedelicraft.id("world_geometry").toString());
            if (OpenGlHelper.glGetProgrami(program.getProgram(), GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                program.deleteShader();
                throw new IOException("The world geometry shader program failed to link");
            }
            shader = program;
        } catch (IOException failure) {
            failed = true;
            LogManager.getLogger(Psychedelicraft.MOD_ID).warn("Could not load the world geometry shader", failure);
        }
    }

    public enum Material {
        TERRAIN(1F, 1F),
        SCREEN(0F, 1F),
        SKY_GRADIENT(0F, 1F),
        END_SKY(0F, 1F),
        END_PORTAL(1F, 1F),
        PARTICLES(1F, 1F),
        ENTITY(1F, 1F),
        ENTITY_GLINT(1F, 1F),
        VANILLA_FAST_CLOUDS(0F, 1F / 8F),
        VANILLA_FANCY_CLOUDS(0F, 1F),
        FORGE_FAST_CLOUDS(2F, 1F / 8F),
        FORGE_FANCY_CLOUDS(2F, 1F / 12F);

        private final float lightmapMode;
        private final float horizontalScale;

        Material(float lightmapMode, float horizontalScale) {
            this.lightmapMode = lightmapMode;
            this.horizontalScale = horizontalScale;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final int previousProgram;
        private final int portalUnit;
        private int previousPortalBinding = -1;

        private Scope(int previousProgram, int portalUnit) {
            this.previousProgram = previousProgram;
            this.portalUnit = portalUnit;
        }

        private void bindPortalAtlas(Minecraft client) {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GlStateManager.setActiveTexture(portalUnit);
            try {
                previousPortalBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                client.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            } finally {
                GlStateManager.setActiveTexture(active);
            }
        }

        @Override
        public void close() {
            OpenGlHelper.glUseProgram(previousProgram);
            if (previousPortalBinding != -1) {
                int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
                GlStateManager.setActiveTexture(portalUnit);
                GlStateManager.bindTexture(previousPortalBinding);
                GlStateManager.setActiveTexture(active);
            }
        }
    }
}
