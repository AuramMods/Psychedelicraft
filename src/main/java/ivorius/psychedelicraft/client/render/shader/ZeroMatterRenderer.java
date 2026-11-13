package ivorius.psychedelicraft.client.render.shader;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.io.IOException;

public final class ZeroMatterRenderer implements IResourceManagerReloadListener {
    public static final ZeroMatterRenderer INSTANCE = new ZeroMatterRenderer();
    private static final ResourceLocation PORTAL = new ResourceLocation("textures/entity/end_portal.png");
    private final ResourceLocation[] screens = new ResourceLocation[8];
    private IResourceManager resources;
    private ShaderManager shader;
    private boolean failed;

    private ZeroMatterRenderer() {
        for (int i = 0; i < screens.length; i++) {
            screens[i] = Psychedelicraft.id("textures/entity/reality_rift/zero_screen_" + i + ".png");
        }
    }

    public void draw(float ticks, float gameTime, Runnable geometry) {
        Minecraft client = Minecraft.getMinecraft();
        ResourceLocation screen = screens[Math.floorMod(MathHelper.floor(ticks * 0.5F), screens.length)];
        prepare(client);

        TextureManager textures = client.getTextureManager();
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int program = OpenGlHelper.shadersSupported ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int previousScreen = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (shader == null) {
            try {
                textures.bindTexture(screen);
                geometry.run();
            } finally {
                GlStateManager.bindTexture(previousScreen);
                GlStateManager.setActiveTexture(active);
            }
            return;
        }

        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        int previousPortal = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean lightmapEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        try {
            textures.bindTexture(screen);
            textures.bindTexture(PORTAL);
            shader.addSamplerTexture("ScreenSampler", textures.getTexture(screen));
            shader.addSamplerTexture("EndPortalSampler", textures.getTexture(PORTAL));
            shader.getShaderUniformOrDefault("GameTime").set(gameTime);
            shader.useShader();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            try (WorldGeometryShader.Scope scope = DrugRenderer.beginZeroMatter(shader)) {
                geometry.run();
            }
        } finally {
            shader.endShader();
            OpenGlHelper.glUseProgram(program);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.bindTexture(previousPortal);
            if (!lightmapEnabled) GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.bindTexture(previousScreen);
            GlStateManager.setActiveTexture(active);
        }
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
        if (shader == null && !failed) {
            try {
                ShaderManager program = new ShaderManager(resources, Psychedelicraft.id("zero_matter").toString());
                if (OpenGlHelper.glGetProgrami(program.getProgram(), GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                    program.deleteShader();
                    throw new IOException("The Zero matter shader program failed to link");
                }
                shader = program;
            } catch (IOException failure) {
                failed = true;
                LogManager.getLogger(Psychedelicraft.MOD_ID).warn("Could not load the Zero matter shader", failure);
            }
        }
    }
}
