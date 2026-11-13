package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

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

    static void render(Minecraft client, int width, int height, int age, float partialTicks, float strength) {
        if (strength <= 0) return;
        Random particles = new Random(age);
        int count = MathHelper.floor(particles.nextFloat() * 200 * strength);
        Random lightning = new Random(age / 2 * 21124871824L);
        int flashes = 0;
        while (lightning.nextFloat() < (strength - 0.5F) * 0.1F && flashes < 3) flashes++;
        if (count == 0 && flashes == 0) return;

        if (count > 0) drawParticles(client, particles, count, width, height, strength);
        if (flashes > 0) drawLightning(client, lightning, flashes, width, height, age, partialTicks, strength);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
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
        DrugRenderer.drawScreenQuads(tessellator);
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
            DrugRenderer.drawScreenQuads(tessellator);
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
