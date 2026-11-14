/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.TranslucentRenderState;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import ivorius.psychedelicraft.client.render.shader.ZeroMatterRenderer;
import ivorius.psychedelicraft.entity.RealityRiftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.Random;

public final class RealityRiftRenderer extends Render<RealityRiftEntity> {
    private static final ResourceLocation CENTER = Psychedelicraft.id("textures/entity/reality_rift/zero_center.png");
    private static final int RAYS = 20;
    private static final float TAU = (float) (2 * Math.PI);
    private static final Vector3f X_AXIS = new Vector3f(1, 0, 0);
    private static final Vector3f Y_AXIS = new Vector3f(0, 1, 0);
    private static final Vector3f Z_AXIS = new Vector3f(0, 0, 1);

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
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.translate(x, y + rift.height * 0.5, z);
            GlStateManager.scale(scale, scale, scale);

            GlStateManager.depthMask(true);
            matter.draw(ticks, time, () -> drawRays(ticks));

            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.1F);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            bindTexture(CENTER);
            GlStateManager.rotate(-renderManager.playerViewY, 0, 1, 0);
            float pitch = renderManager.playerViewX;
            GlStateManager.rotate(Minecraft.getMinecraft().gameSettings.thirdPersonView == 2 ? -pitch : pitch, 1, 0, 0);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            vertex(buffer, -2.5, -2.5, 0, 0, 0, 1);
            vertex(buffer, 2.5, -2.5, 0, 1, 0, 1);
            vertex(buffer, 2.5, 2.5, 0, 1, 1, 1);
            vertex(buffer, -2.5, 2.5, 0, 0, 1, 1);
            try (Scope scope = DrugRenderer.beginRiftCenter()) {
                tessellator.draw();
            }
        } finally {
            GlStateManager.bindTexture(texture);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            if (lightmap) GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(activeTexture);
            GlStateManager.alphaFunc(alphaFunc, alphaRef);
            GlStateManager.depthFunc(depthFunc);
        }
        super.doRender(rift, x, y, z, yaw, partialTicks);
    }

    private static void drawRays(float ticks) {
        Random shape = new Random(432);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f rotation = new Matrix4f();
        Vector4f position = new Vector4f();
        float twist = ticks / 200F * ((float) Math.PI / 2) * 0.5F;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int ray = 0; ray < RAYS; ray++) {
            float wave = ((float) ray / RAYS * 28493 + ticks) / 10 % 20;
            if (wave > 10) wave = 20 - wave;
            float alpha = 1F / (1F + (float) Math.pow(2.71828F, -0.8F * wave) * (1F / 0.01F - 1));
            if (alpha <= 0.01F) continue;

            rotation.rotate(shape.nextFloat() * TAU, X_AXIS);
            rotation.rotate(shape.nextFloat() * TAU, Y_AXIS);
            rotation.rotate(shape.nextFloat() * TAU, Z_AXIS);
            rotation.rotate(shape.nextFloat() * TAU, X_AXIS);
            rotation.rotate(shape.nextFloat() * TAU, Y_AXIS);
            rotation.rotate(shape.nextFloat() * TAU + twist, Z_AXIS);
            float length = shape.nextFloat() * 20 + 5;
            float width = shape.nextFloat() * 2 + 1;
            // A single QUADS batch groups consecutive vertices across the five-vertex ray boundaries.
            vertex(buffer, 0, 0, 0, 0, 0, alpha);
            rayTip(buffer, rotation, position, -2.5F * width, length, -0.5F * width, 1, 0);
            rayTip(buffer, rotation, position, 2.5F * width, length, -0.5F * width, 0, 1);
            rayTip(buffer, rotation, position, 0, length, width, 1, 1);
            rayTip(buffer, rotation, position, -2.5F * width, length, -0.5F * width, 1, 1);
        }
        tessellator.draw();
    }

    private static void rayTip(BufferBuilder buffer, Matrix4f rotation, Vector4f position, float x, float y, float z, int u, int v) {
        position.set(x, y, z, 1);
        Matrix4f.transform(rotation, position, position);
        vertex(buffer, position.x, position.y, position.z, u, v, 0);
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, double u, double v, float alpha) {
        buffer.pos(x, y, z).tex(u, v).color(1F, 1F, 1F, alpha).endVertex();
    }
}
