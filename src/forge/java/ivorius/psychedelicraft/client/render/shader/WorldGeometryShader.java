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
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
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

    @Nullable
    public Scope begin(Minecraft client, EntityPlayerSP player, HallucinationManager hallucinations,
                       DrugProperties drugs, Material material, float cloudY) {
        float small = hallucinations.strength(Effect.SMALL_WAVES) * 0.5F;
        float big = hallucinations.strength(Effect.BIG_WAVES) * 0.6F;
        float wiggle = hallucinations.strength(Effect.WIGGLE_WAVES) * 0.7F;
        float distant = hallucinations.strength(Effect.DISTANT_WORLD_DEFORMATION);
        float surface = MathHelper.clamp(hallucinations.strength(Effect.SURFACE_FRACTALS), 0F, 1F);
        float fracture = (float) drugs.getActiveValue(DrugType.LSD) * 0.06F;
        float[] pulses = hallucinations.pulseColor();
        if (small <= 0F && big <= 0F && wiggle <= 0F && distant <= 0F
                && surface <= 0F && fracture <= 0F && pulses == null) return null;

        prepare(client);
        if (shader == null) return null;

        Scope scope = new Scope(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        try {
            // The caller owns the primary textures, alpha test, blend and depth passes.
            OpenGlHelper.glUseProgram(shader.getProgram());
            setSampler("ColorSampler", 0);
            setSampler("LightmapSampler", 1);
            setSampler("PortalSampler", material == Material.TERRAIN ? 0 : 2);
            upload("WorldTicks", player.ticksExisted + client.getRenderPartialTicks());
            upload("DistantDeformation", distant);
            upload("SurfaceFractalStrength", surface);
            upload("FractureStrength", fracture);
            upload("FogMode", fogMode());
            upload("RadialFog", radialFog() ? 1F : 0F);
            upload("LightmapMode", material.lightmapMode);

            ShaderUniform coordinates = shader.getShaderUniformOrDefault("GeometryCoordinates");
            coordinates.set(material.horizontalScale, cloudY);
            coordinates.upload();

            ShaderUniform waves = shader.getShaderUniformOrDefault("Waves");
            waves.set(small, big, wiggle);
            waves.upload();
            ShaderUniform position = shader.getShaderUniformOrDefault("PlayerXZ");
            position.set((float) player.posX, (float) player.posZ);
            position.upload();
            ShaderUniform pulseColor = shader.getShaderUniformOrDefault("Pulses");
            if (pulses == null) pulseColor.set(0F, 0F, 0F, 0F);
            else pulseColor.set(pulses);
            pulseColor.upload();
            if (surface > 0F) {
                if (material != Material.TERRAIN) scope.bindPortalAtlas(client);
                TextureAtlasSprite sprite = client.getBlockRendererDispatcher().getBlockModelShapes()
                        .getTexture(Blocks.PORTAL.getDefaultState());
                ShaderUniform coords = shader.getShaderUniformOrDefault("SurfaceFractalCoords");
                coords.set(sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV());
                coords.upload();
            }
            return scope;
        } catch (RuntimeException | Error failure) {
            scope.close();
            throw failure;
        }
    }

    private void setSampler(String name, int unit) {
        OpenGlHelper.glUniform1i(OpenGlHelper.glGetUniformLocation(shader.getProgram(), name), unit);
    }

    private void upload(String name, float value) {
        ShaderUniform uniform = shader.getShaderUniformOrDefault(name);
        uniform.set(value);
        uniform.upload();
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
        PARTICLES(1F, 1F),
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
        private int previousPortalBinding = -1;

        private Scope(int previousProgram) {
            this.previousProgram = previousProgram;
        }

        private void bindPortalAtlas(Minecraft client) {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE2);
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
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE2);
                GlStateManager.bindTexture(previousPortalBinding);
                GlStateManager.setActiveTexture(active);
            }
        }
    }
}
