package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.Random;

final class PowerOverlay {
    private static final ResourceLocation PARTICLE = Psychedelicraft.id("textures/drug/power/particle.png");
    private static final ResourceLocation[] LIGHTNING = {
            Psychedelicraft.id("textures/drug/power/lightning_0.png"),
            Psychedelicraft.id("textures/drug/power/lightning_1.png"),
            Psychedelicraft.id("textures/drug/power/lightning_2.png"),
            Psychedelicraft.id("textures/drug/power/lightning_3.png")
    };

    private PowerOverlay() {
    }

    static void render(Minecraft client, int age, float partialTicks, float strength) {
        if (strength <= 0) return;
        Random particles = new Random(age);
        int count = MathHelper.floor(particles.nextFloat() * 200 * strength);
        Random lightning = new Random(age / 2 * 21124871824L);
        int flashes = 0;
        while (lightning.nextFloat() < (strength - 0.5F) * 0.1F && flashes < 3) flashes++;
        if (count == 0 && flashes == 0) return;

        ScaledResolution resolution = new ScaledResolution(client);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int program = OpenGlHelper.shadersSupported ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, width, height, 0, -1, 1);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.loadIdentity();
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            if (OpenGlHelper.shadersSupported) OpenGlHelper.glUseProgram(0);

            if (count > 0) drawParticles(client, particles, count, width, height, strength);
            if (flashes > 0) drawLightning(client, lightning, flashes, width, height, age, partialTicks, strength);
        } finally {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(matrixMode);
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (OpenGlHelper.shadersSupported) OpenGlHelper.glUseProgram(program);
            GlStateManager.bindTexture(boundTexture);
            if (!texture) GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            if (lightmap) GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(activeTexture);
        }
    }

    private static void drawParticles(Minecraft client, Random random, int count, int width, int height, float strength) {
        int particleWidth = Math.max(1, height / 10);
        int particleHeight = MathHelper.ceil(particleWidth * strength);
        client.getTextureManager().bindTexture(PARTICLE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertices = tessellator.getBuffer();
        vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(width + particleWidth) - particleWidth;
            int y = random.nextInt(height + particleHeight) - particleHeight;
            quad(vertices, x, y, particleWidth, particleHeight, false, 1);
        }
        tessellator.draw();
    }

    private static void drawLightning(Minecraft client, Random random, int count, int width, int height,
                                      int age, float partialTicks, float strength) {
        float alpha = (0.05F + strength * 0.1F) * (1 - ((age % 2) + partialTicks) * 0.5F);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertices = tessellator.getBuffer();
        for (int i = 0; i < count; i++) {
            float x = random.nextInt(width + height) - height
                    + (random.nextFloat() - 0.5F) * height * partialTicks * 2;
            client.getTextureManager().bindTexture(LIGHTNING[random.nextInt(LIGHTNING.length)]);
            boolean inverted = random.nextBoolean();
            vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            quad(vertices, x, 0, height, height, inverted, alpha);
            tessellator.draw();
        }
    }

    private static void quad(BufferBuilder buffer, float x, float y, float width, float height, boolean inverted, float alpha) {
        int bottom = inverted ? 0 : 1;
        int top = 1 - bottom;
        buffer.pos(x, y + height, 0).tex(0, bottom).color(1F, 1F, 1F, alpha).endVertex();
        buffer.pos(x + width, y + height, 0).tex(1, bottom).color(1F, 1F, 1F, alpha).endVertex();
        buffer.pos(x + width, y, 0).tex(1, top).color(1F, 1F, 1F, alpha).endVertex();
        buffer.pos(x, y, 0).tex(0, top).color(1F, 1F, 1F, alpha).endVertex();
    }
}
