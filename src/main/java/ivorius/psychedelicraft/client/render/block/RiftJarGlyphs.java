/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.TranslucentRenderState;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

final class RiftJarGlyphs {
    private static final ResourceLocation FONT = new ResourceLocation("textures/font/ascii_sga.png");
    private static final String SPIRAL_TEXT = "This is a small spiral.";
    private static final String CIRCLE_TEXT = "This is a small circle.";
    private static final RiftGlyphPath OUTGOING = RiftGlyphPath.spiral(new Vec3d(0, 6, 0), 0.06, 6, 6);
    private static final RiftGlyphPath SPHERE = RiftGlyphPath.sphere(3, 8);
    private static final float SCALE = (-1 / 12F) * 0.2F;

    private final Map<RiftJarBlockEntity.Connection, RiftGlyphPath> paths = new WeakHashMap<>();

    void render(RiftJarBlockEntity jar, double x, double y, double z, float ticks, float outgoing) {
        if (jar.getConnections().isEmpty() && outgoing <= 0) return;

        Minecraft client = Minecraft.getMinecraft();
        float blockLight = OpenGlHelper.lastBrightnessX;
        float skyLight = OpenGlHelper.lastBrightnessY;
        int light = jar.getWorld().getCombinedLight(jar.getPos(), 0);
        int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.translate(x, y, z);
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.1F);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);

            client.getTextureManager().bindTexture(FONT);
            float shift = ticks * -0.002F;
            try (Scope scope = DrugRenderer.beginWorldModel()) {
                for (RiftJarBlockEntity.Connection connection : jar.getConnections()) {
                    Vec3d target = connection.getPosition().subtract(jar.getPos().getX() + 0.5,
                            jar.getPos().getY() + 0.6, jar.getPos().getZ() + 0.5);
                    float strength = connection.getStrength();
                    RiftGlyphPath path = paths.computeIfAbsent(connection,
                            ignoredConnection -> RiftGlyphPath.spiral(target, 0.1, 0.5, 8));
                    draw(SPIRAL_TEXT, path, shift, strength, 0);
                    GlStateManager.pushMatrix();
                    try {
                        GlStateManager.translate(target.x, target.y, target.z);
                        draw(CIRCLE_TEXT, SPHERE, shift, 1, 1 - strength);
                    } finally {
                        GlStateManager.popMatrix();
                    }
                }
                if (outgoing > 0) draw(SPIRAL_TEXT, OUTGOING, shift, outgoing, 0);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, blockLight, skyLight);
            GlStateManager.depthFunc(depthFunc);
            GlStateManager.alphaFunc(alphaFunc, alphaRef);
        }
    }

    private void draw(String label, RiftGlyphPath path, float shift, float cap, float hidden) {
        Random mask = new Random(42);
        for (int index = 0; index < label.length(); index++) {
            char glyph = label.charAt(index);
            // Masked characters and spaces still occupy a position along the path.
            boolean masked = hidden > 0 && mask.nextFloat() <= hidden;
            double progress = ((index / (float) label.length() + shift) % 1D + 1) % 1D;
            if (masked || glyph == ' ' || progress > cap) continue;

            Vec3d point = path.at(progress);
            Vec3d tangent = path.at((float) progress + 0.003).subtract(point);
            double length = tangent.lengthVector();
            // The donor converts the tangent to these angles before adding the outward-facing yaw.
            float yaw = (float) (Math.atan2(tangent.y, tangent.z) / (Math.PI * 180)) + 180F;
            float pitch = length == 0 ? 90F
                    : (float) (Math.acos(MathHelper.clamp(tangent.x / length, -1, 1)) / (Math.PI * 180) + 90);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(point.x, point.y, point.z);
                GlStateManager.scale(SCALE, SCALE, SCALE);
                GlStateManager.rotate(yaw, 0, 1, 0);
                GlStateManager.rotate(pitch, 1, 0, 0);
                drawGlyph(glyph);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }

    private static void drawGlyph(char glyph) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        if (glyph == '.') {
            // The donor's alt font uses the standard missing glyph for the final period.
            GlStateManager.bindTexture(MissingGlyph.TEXTURE.getGlTextureId());
            quad(buffer, 0, 0, 5, 8, 0.01 / 5, 0.01 / 8, 4.99 / 5, 7.99 / 8);
        } else {
            int u = 8 * (glyph % 16);
            int v = 8 * (glyph / 16);
            quad(buffer, 0, 0, 8, 8, (u + 0.01) / 128, (v + 0.01) / 128,
                    (u + 7.99) / 128, (v + 7.99) / 128);
        }
        tessellator.draw();
        if (glyph == '.') Minecraft.getMinecraft().getTextureManager().bindTexture(FONT);
    }

    private static final class MissingGlyph {
        private static final DynamicTexture TEXTURE = create();

        private static DynamicTexture create() {
            DynamicTexture texture = new DynamicTexture(5, 8);
            int[] pixels = texture.getTextureData();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 5; x++) {
                    if (x == 0 || x == 4 || y == 0 || y == 7) pixels[y * 5 + x] = 0xFFFFFFFF;
                }
            }
            texture.updateDynamicTexture();
            return texture;
        }
    }

    private static void quad(BufferBuilder buffer, int left, int top, int right, int bottom,
                             double u0, double v0, double u1, double v1) {
        buffer.pos(left, top, 0).tex(u0, v0).color(255, 255, 255, 255).endVertex();
        buffer.pos(left, bottom, 0).tex(u0, v1).color(255, 255, 255, 255).endVertex();
        buffer.pos(right, bottom, 0).tex(u1, v1).color(255, 255, 255, 255).endVertex();
        buffer.pos(right, top, 0).tex(u1, v0).color(255, 255, 255, 255).endVertex();
    }
}
