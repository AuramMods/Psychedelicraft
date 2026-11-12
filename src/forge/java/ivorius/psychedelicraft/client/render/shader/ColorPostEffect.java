package ivorius.psychedelicraft.client.render.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public final class ColorPostEffect implements IResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger(Psychedelicraft.MOD_ID);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final ByteBuffer colorMask = BufferUtils.createByteBuffer(16);
    private final FloatBuffer clearColor = BufferUtils.createFloatBuffer(16);

    private IResourceManager resources;
    private ShaderManager shader;
    private Framebuffer target;
    private boolean failed;

    public void render(Minecraft client, float ticks, HallucinationManager hallucinations, float intensify, float desaturate) {
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) {
            return;
        }
        float slow = hallucinations.strength(Effect.SLOW_COLOR_ROTATION);
        float quick = hallucinations.strength(Effect.QUICK_COLOR_ROTATION);
        if (slow == 0F && quick == 0F && intensify == 0F && desaturate == 0F) {
            return;
        }
        if (resources != client.getResourceManager()) {
            resources = client.getResourceManager();
            ((IReloadableResourceManager) resources).registerReloadListener(this);
        }
        if (failed) {
            return;
        }

        int framebuffer = currentFramebuffer();
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clearColor);

        try {
            Framebuffer main = client.getFramebuffer();
            if (shader == null) {
                shader = new ShaderManager(resources, Psychedelicraft.id("simple_effects").toString());
            }
            if (target == null) {
                target = new Framebuffer(main.framebufferWidth, main.framebufferHeight, false);
            } else if (target.framebufferWidth != main.framebufferWidth || target.framebufferHeight != main.framebufferHeight) {
                target.createBindFramebuffer(main.framebufferWidth, main.framebufferHeight);
            }

            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.colorMask(true, true, true, true);

            draw(main, target, ticks, slow, quick, intensify, desaturate);
            // Copy back with neutral uniforms, without clearing the world's depth buffer.
            draw(target, main, ticks, 0F, 0F, 0F, 0F);
        } catch (IOException | RuntimeException ex) {
            failed = true;
            LOGGER.warn("Could not render Psychedelicraft color effects", ex);
            releaseTarget();
        } finally {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GlStateManager.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            OpenGlHelper.glUseProgram(program);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.bindTexture(boundTexture);
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(activeTexture);
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            GlStateManager.depthMask(depthMask);
            GlStateManager.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0,
                    colorMask.get(2) != 0, colorMask.get(3) != 0);
            GlStateManager.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
        }
    }

    private void draw(Framebuffer input, Framebuffer output, float ticks, float slow, float quick,
                      float intensify, float desaturate) {
        output.bindFramebuffer(true);
        shader.addSamplerTexture("DiffuseSampler", input);
        shader.getShaderUniformOrDefault("Ticks").set(ticks);
        shader.getShaderUniformOrDefault("ColorStrengths").set(slow, quick, intensify, desaturate);
        shader.getShaderUniformOrDefault("TextureScale").set(
                (float) input.framebufferWidth / input.framebufferTextureWidth,
                (float) input.framebufferHeight / input.framebufferTextureHeight);

        try {
            shader.useShader();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertices = tessellator.getBuffer();
            vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            vertices.pos(-1, -1, 0).endVertex();
            vertices.pos(1, -1, 0).endVertex();
            vertices.pos(1, 1, 0).endVertex();
            vertices.pos(-1, 1, 0).endVertex();
            tessellator.draw();
        } finally {
            shader.endShader();
        }
    }

    public void releaseTarget() {
        if (target != null && OpenGlHelper.isFramebufferEnabled()) {
            int framebuffer = currentFramebuffer();
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int restoreFramebuffer = framebuffer == target.framebufferObject ? 0 : framebuffer;
            int restoreTexture = texture == target.framebufferTexture ? 0 : texture;
            target.deleteFramebuffer();
            target = null;
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, restoreFramebuffer);
            GlStateManager.bindTexture(restoreTexture);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        releaseTarget();
        if (shader != null) {
            shader.deleteShader();
            shader = null;
        }
        failed = false;
    }

    private static int currentFramebuffer() {
        return GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    }
}
