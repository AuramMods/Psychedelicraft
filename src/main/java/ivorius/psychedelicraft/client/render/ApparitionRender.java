package ivorius.psychedelicraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

public final class ApparitionRender {
    private static final FloatBuffer TINT = BufferUtils.createFloatBuffer(4);
    private static Entity rendering;

    private ApparitionRender() {
    }

    static boolean isRendering(Entity entity) {
        return entity == rendering;
    }

    static void drawEntity(Minecraft client, Entity entity, Vec3d[] offsets, Vec3d center,
                           float yaw, float partial, float scale, float red, float green, float blue, float alpha) {
        RenderManager manager = client.getRenderManager();
        boolean shadows = manager.isRenderShadow();
        rendering = entity;
        manager.setRenderShadow(false);
        client.entityRenderer.enableLightmap();
        RenderHelper.enableStandardItemLighting();
        int light = entity.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
        GlStateManager.color(1F, 1F, 1F, 1F);
        setTint(red, green, blue, alpha);
        try {
            for (Vec3d offset : offsets) {
                Vec3d position = center.add(offset.scale(scale));
                prepareBlend();
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(position.x, position.y, position.z);
                    GlStateManager.scale(scale, scale, scale);
                    manager.renderEntity(entity, 0, 0, 0, yaw, partial, true);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        } finally {
            clearTint();
            rendering = null;
            manager.setRenderShadow(shadows);
            RenderHelper.disableStandardItemLighting();
            client.entityRenderer.disableLightmap();
            restoreBlend();
        }
    }

    static void drawModel(ApparitionModels model, Vec3d[] offsets, Vec3d center, float yaw, float age,
                          double horizontalSpeedSquared, float scale,
                          float red, float green, float blue, float alpha) {
        prepareBlend();
        GlStateManager.disableTexture2D();
        try {
            for (Vec3d offset : offsets) {
                Vec3d position = center.add(offset.scale(scale));
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(position.x, position.y, position.z);
                    GlStateManager.rotate(180F - yaw + model.swimYaw(age), 0, 1, 0);
                    GlStateManager.scale(scale, scale, scale);
                    model.draw(age, horizontalSpeedSquared, red, green, blue, alpha);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        } finally {
            GlStateManager.enableTexture2D();
            restoreBlend();
        }
    }

    // Native layers can reset blend or depth writes before drawing their geometry.
    public static void prepareLayer() {
        if (rendering != null) prepareBlend();
    }

    private static void prepareBlend() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 1F / 255);
    }

    private static void restoreBlend() {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }

    // Use the legacy renderer's spare texture unit so model layers can set their own colors.
    private static void setTint(float red, float green, float blue, float alpha) {
        GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(TextureUtil.MISSING_TEXTURE.getGlTextureId());
        textureEnv(GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        textureEnv(GL13.GL_COMBINE_RGB, GL11.GL_MODULATE);
        textureEnv(GL13.GL_SOURCE0_RGB, GL13.GL_PREVIOUS);
        textureEnv(GL13.GL_SOURCE1_RGB, GL13.GL_CONSTANT);
        textureEnv(GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        textureEnv(GL13.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
        textureEnv(GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        textureEnv(GL13.GL_SOURCE0_ALPHA, GL13.GL_PREVIOUS);
        textureEnv(GL13.GL_SOURCE1_ALPHA, GL13.GL_CONSTANT);
        textureEnv(GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        textureEnv(GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
        TINT.clear();
        TINT.put(red).put(green).put(blue).put(alpha).flip();
        GlStateManager.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, TINT);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private static void clearTint() {
        GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
        textureEnv(GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GlStateManager.bindTexture(0);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private static void textureEnv(int parameter, int value) {
        GlStateManager.glTexEnvi(GL11.GL_TEXTURE_ENV, parameter, value);
    }
}
