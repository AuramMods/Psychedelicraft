/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.TranslucentRenderState;
import ivorius.psychedelicraft.client.render.shader.ZeroMatterRenderer;
import ivorius.psychedelicraft.entity.RealityRiftEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public final class RealityRiftRenderer extends Render<RealityRiftEntity> {
    private static final ResourceLocation CENTER = Psychedelicraft.id("textures/entity/reality_rift/zero_center.png");
    private static final int RAYS = 20;

    public RealityRiftRenderer(RenderManager manager) {
        super(manager);
    }

    @Override
    protected ResourceLocation getEntityTexture(RealityRiftEntity entity) {
        return CENTER;
    }

    @Override
    public void doRender(RealityRiftEntity rift, double x, double y, double z, float yaw, float partialTicks) {
        float size = rift.getVisualSize(partialTicks);
        if (size <= 0) return;
        float scale = size < 0.01F ? size * 10 : 0.1F + (size - 0.01F) * 0.1F;
        float instability = rift.getInstability();
        float ticks = rift.ticksExisted + partialTicks + instability * instability * 3000;
        float time = ((rift.world.getTotalWorldTime() % 24000) + partialTicks) / 24000F;
        ZeroMatterRenderer matter = ZeroMatterRenderer.INSTANCE;
        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;

        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.translate(x, y + rift.height * 0.5, z);
            GlStateManager.scale(scale, scale, scale);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            GlStateManager.pushMatrix();
            try {
                matter.draw(ticks, time, () -> drawRays(ticks));
            } finally {
                GlStateManager.popMatrix();
            }

            bindTexture(CENTER);
            GlStateManager.rotate(-renderManager.playerViewY, 0, 1, 0);
            GlStateManager.rotate(renderManager.playerViewX, 1, 0, 0);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            vertex(buffer, -2.5, -2.5, 0, 0, 1, 1);
            vertex(buffer, 2.5, -2.5, 0, 1, 1, 1);
            vertex(buffer, 2.5, 2.5, 0, 1, 0, 1);
            vertex(buffer, -2.5, 2.5, 0, 0, 0, 1);
            tessellator.draw();
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        }
        super.doRender(rift, x, y, z, yaw, partialTicks);
    }

    private static void drawRays(float ticks) {
        Random shape = new Random(432);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        for (int ray = 0; ray < RAYS; ray++) {
            float wave = ((float) ray / RAYS * 28493 + ticks) / 10 % 20;
            if (wave > 10) wave = 20 - wave;
            float alpha = (float) (1 / (1 + Math.exp(-0.8F * wave) * 99));
            if (alpha <= 0.01F) continue;

            GlStateManager.rotate(shape.nextFloat() * 360, 1, 0, 0);
            GlStateManager.rotate(shape.nextFloat() * 360, 0, 1, 0);
            GlStateManager.rotate(shape.nextFloat() * 360, 0, 0, 1);
            GlStateManager.rotate(shape.nextFloat() * 360, 1, 0, 0);
            GlStateManager.rotate(shape.nextFloat() * 360, 0, 1, 0);
            GlStateManager.rotate(shape.nextFloat() * 360 + ticks * 45 / 200, 0, 0, 1);
            float length = shape.nextFloat() * 20 + 5;
            float width = shape.nextFloat() * 2 + 1;
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX_COLOR);
            vertex(buffer, 0, 0, 0, 0, 0, alpha);
            vertex(buffer, -2.5 * width, length, -0.5 * width, 1, 0, 0);
            vertex(buffer, 2.5 * width, length, -0.5 * width, 0, 1, 0);
            vertex(buffer, 0, length, width, 1, 1, 0);
            vertex(buffer, -2.5 * width, length, -0.5 * width, 1, 0, 0);
            tessellator.draw();
        }
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, double u, double v, float alpha) {
        buffer.pos(x, y, z).tex(u, v).color(1F, 1F, 1F, alpha).endVertex();
    }
}
