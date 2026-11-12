package ivorius.psychedelicraft.client.render.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.EnvironmentEffects;
import ivorius.psychedelicraft.client.render.HallucinationManager;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nullable;

public final class ColorPostEffect implements IResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger(Psychedelicraft.MOD_ID);
    private static final ResourceLocation HEAT_NOISE = Psychedelicraft.id("textures/environment/heat_distortion_noise.png");
    private static final ResourceLocation WATER_NOISE = Psychedelicraft.id("textures/environment/water_distortion.png");
    private static final ResourceLocation HURT_OVERLAY = Psychedelicraft.id("textures/environment/hurt_overlay.png");
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final ByteBuffer colorMask = BufferUtils.createByteBuffer(16);
    private final FloatBuffer clearColor = BufferUtils.createFloatBuffer(16);
    private final int[] boundTextures = new int[3];
    private final boolean[] enabledTextures = new boolean[3];

    private IResourceManager resources;
    private ShaderManager shader;
    private ShaderManager coloredBloom;
    private ShaderManager distortion;
    private ShaderManager wetScreen;
    private ShaderManager hurtScreen;
    private Framebuffer target;
    private int depthTexture;
    private int depthWidth;
    private int depthHeight;
    private boolean failed;

    public void render(Minecraft client, float partialTicks, HallucinationManager hallucinations, DrugProperties drugs,
                       EnvironmentEffects environment) {
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) {
            return;
        }
        float slow = hallucinations.strength(Effect.SLOW_COLOR_ROTATION);
        float quick = hallucinations.strength(Effect.QUICK_COLOR_ROTATION);
        float intensify = drugs.getColorIntensification(hallucinations.strength(Effect.SUPER_SATURATION));
        float desaturate = drugs.getDesaturation(hallucinations.strength(Effect.DESATURATION));
        HarmoniumDrug harmonium = drugs.getHarmonium();
        float harmoniumStrength = harmonium == null ? 0F : (float) harmonium.getActiveValue();
        float heat = environment.heatDistortion();
        float water = environment.waterDistortion((float) drugs.getActiveValue(DrugType.PEYOTE));
        float wet = environment.wetOverlay();
        float hurt = environment.hurtOverlay(client.player.hurtTime, client.player.maxHurtTime, drugs.heartbeatPulse(partialTicks));
        boolean colors = slow != 0F || quick != 0F || intensify != 0F || desaturate != 0F;
        if (!colors && harmoniumStrength == 0F && heat == 0F && water == 0F && wet == 0F && hurt == 0F) {
            return;
        }
        prepareResources(client);
        if (failed) {
            return;
        }

        int framebuffer = currentFramebuffer();
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int textureUnits = heat > 0F || water > 0F ? 3 : 2;
        for (int unit = 0; unit < textureUnits; unit++) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
            boundTextures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            enabledTextures[unit] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        }
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
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

            float[] color = harmoniumStrength > 0F ? harmonium.color() : null;
            boolean hasDepth = depthTexture != 0 && depthWidth == main.framebufferTextureWidth
                    && depthHeight == main.framebufferTextureHeight;
            float ticks = client.player.ticksExisted + partialTicks;
            Framebuffer source = main;
            if (hurt > 0F) {
                drawHurt(client, source, target, hurt);
                source = target;
            }
            if (hasDepth && heat > 0F) {
                Framebuffer output = otherTarget(source, main);
                drawDistortion(client, source, output, heat, ticks * 0.15F);
                source = output;
            }
            if (hasDepth && water > 0F) {
                Framebuffer output = otherTarget(source, main);
                drawDistortion(client, source, output, water, ticks * 0.03F);
                source = output;
            }
            if (colors || (hasDepth && color != null)) {
                Framebuffer output = otherTarget(source, main);
                drawColors(source, output, ticks, slow, quick, intensify, desaturate, hasDepth ? color : null, harmoniumStrength);
                source = output;
            }
            if (color != null) renderColoredBloom(source, otherTarget(source, main), color, harmoniumStrength * 3F);
            if (wet > 0F) {
                Framebuffer output = otherTarget(source, main);
                drawWetScreen(client, source, output, wet, ticks);
                source = output;
            }
            // Copy back with neutral uniforms, leaving the world's depth buffer intact.
            if (source != main) drawColors(source, main, ticks, 0F, 0F, 0F, 0F, null, 0F);
        } catch (IOException | RuntimeException ex) {
            failed = true;
            LOGGER.warn("Could not render Psychedelicraft screen effects", ex);
            releaseTarget();
        } finally {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GlStateManager.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            OpenGlHelper.glUseProgram(program);
            for (int unit = 0; unit < textureUnits; unit++) {
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
                GlStateManager.bindTexture(boundTextures[unit]);
                if (enabledTextures[unit]) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            }
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

    private Framebuffer otherTarget(Framebuffer source, Framebuffer main) {
        return source == main ? target : main;
    }

    private void prepareResources(Minecraft client) {
        if (resources != client.getResourceManager()) {
            resources = client.getResourceManager();
            ((IReloadableResourceManager) resources).registerReloadListener(this);
        }
    }

    public void captureDepth(Minecraft client) {
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) return;
        prepareResources(client);
        if (failed) return;

        int framebuffer = currentFramebuffer();
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            // Forge dispatches RenderWorldLast before the hand pass clears world depth.
            copyDepth(client.getFramebuffer());
        } finally {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GlStateManager.bindTexture(boundTexture);
            GlStateManager.setActiveTexture(activeTexture);
        }
    }

    private void copyDepth(Framebuffer input) {
        input.bindFramebuffer(false);
        if (depthTexture == 0) depthTexture = TextureUtil.glGenTextures();
        GlStateManager.bindTexture(depthTexture);
        if (depthWidth != input.framebufferTextureWidth || depthHeight != input.framebufferTextureHeight) {
            depthWidth = input.framebufferTextureWidth;
            depthHeight = input.framebufferTextureHeight;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, depthWidth, depthHeight, 0,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, (IntBuffer) null);
        }
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, input.framebufferWidth, input.framebufferHeight);
    }

    private void drawColors(Framebuffer input, Framebuffer output, float ticks, float slow, float quick,
                            float intensify, float desaturate, @Nullable float[] color, float strength) throws IOException {
        if (shader == null) shader = program("simple_effects");
        shader.addSamplerTexture("DepthSampler", color == null ? input : depthTexture);
        shader.getShaderUniformOrDefault("Ticks").set(ticks);
        shader.getShaderUniformOrDefault("ColorStrengths").set(slow, quick, intensify, desaturate);
        if (color == null) shader.getShaderUniformOrDefault("WorldColor").set(0F, 0F, 0F, 0F);
        else shader.getShaderUniformOrDefault("WorldColor").set(color[0], color[1], color[2], strength);
        draw(shader, input, output);
    }

    private void renderColoredBloom(Framebuffer input, Framebuffer scratch, float[] color, float strength) throws IOException {
        if (coloredBloom == null) coloredBloom = program("colored_bloom");
        coloredBloom.getShaderUniformOrDefault("BloomColor").set(color[0], color[1], color[2]);
        for (int pass = 0; pass < MathHelper.ceil(strength); pass++) {
            coloredBloom.getShaderUniformOrDefault("TotalAlpha").set(Math.max(1F, strength - pass));
            coloredBloom.getShaderUniformOrDefault("Direction").set(1F / input.framebufferTextureWidth, 0F);
            draw(coloredBloom, input, scratch);
            coloredBloom.getShaderUniformOrDefault("Direction").set(0F, 1F / scratch.framebufferTextureHeight);
            draw(coloredBloom, scratch, input);
        }
    }

    private void drawDistortion(Minecraft client, Framebuffer input, Framebuffer output, float strength, float ticks) throws IOException {
        if (distortion == null) distortion = program("heat_distortion");
        distortion.addSamplerTexture("DepthSampler", depthTexture);
        distortion.addSamplerTexture("NoiseSampler", texture(client, HEAT_NOISE));
        distortion.getShaderUniformOrDefault("Strength").set(strength);
        distortion.getShaderUniformOrDefault("Ticks").set(ticks);
        draw(distortion, input, output);
    }

    private void drawWetScreen(Minecraft client, Framebuffer input, Framebuffer output, float strength, float ticks) throws IOException {
        if (wetScreen == null) wetScreen = program("wet_screen");
        wetScreen.addSamplerTexture("NoiseSampler", texture(client, WATER_NOISE));
        wetScreen.getShaderUniformOrDefault("Wetness").set(strength);
        wetScreen.getShaderUniformOrDefault("Ticks").set(ticks);
        draw(wetScreen, input, output);
    }

    private void drawHurt(Minecraft client, Framebuffer input, Framebuffer output, float opacity) throws IOException {
        if (hurtScreen == null) hurtScreen = program("hurt_screen");
        hurtScreen.addSamplerTexture("OverlaySampler", texture(client, HURT_OVERLAY));
        hurtScreen.getShaderUniformOrDefault("Opacity").set(opacity);
        ScaledResolution resolution = new ScaledResolution(client);
        float width = resolution.getScaledWidth();
        float height = resolution.getScaledHeight();
        int offset = (int) ((1F - opacity) * 40F);
        hurtScreen.getShaderUniformOrDefault("OverlayScale").set(width / (width + 2 * offset), height / (height + 2 * offset));
        draw(hurtScreen, input, output);
    }

    private ShaderManager program(String name) throws IOException {
        return new ShaderManager(resources, Psychedelicraft.id(name).toString());
    }

    private static ITextureObject texture(Minecraft client, ResourceLocation location) {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        client.getTextureManager().bindTexture(location);
        return client.getTextureManager().getTexture(location);
    }

    private void draw(ShaderManager program, Framebuffer input, Framebuffer output) {
        output.bindFramebuffer(true);
        program.addSamplerTexture("DiffuseSampler", input);
        program.getShaderUniformOrDefault("TextureScale").set(
                (float) input.framebufferWidth / input.framebufferTextureWidth,
                (float) input.framebufferHeight / input.framebufferTextureHeight);

        try {
            program.useShader();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertices = tessellator.getBuffer();
            vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            vertices.pos(-1, -1, 0).endVertex();
            vertices.pos(1, -1, 0).endVertex();
            vertices.pos(1, 1, 0).endVertex();
            vertices.pos(-1, 1, 0).endVertex();
            tessellator.draw();
        } finally {
            program.endShader();
        }
    }

    public void releaseTarget() {
        if (depthTexture != 0) {
            TextureUtil.deleteTexture(depthTexture);
            depthTexture = 0;
            depthWidth = 0;
            depthHeight = 0;
        }
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
        if (coloredBloom != null) {
            coloredBloom.deleteShader();
            coloredBloom = null;
        }
        if (distortion != null) {
            distortion.deleteShader();
            distortion = null;
        }
        if (wetScreen != null) {
            wetScreen.deleteShader();
            wetScreen = null;
        }
        if (hurtScreen != null) {
            hurtScreen.deleteShader();
            hurtScreen = null;
        }
        failed = false;
    }

    private static int currentFramebuffer() {
        return GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    }
}
