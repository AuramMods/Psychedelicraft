package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.client.render.TranslucentRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.function.DoubleFunction;

final class RiftJarGlyphs {
    private static final ResourceLocation FONT = new ResourceLocation("textures/font/ascii_sga.png");
    private static final String SPIRAL_TEXT = "This is a small spiral.";
    private static final String CIRCLE_TEXT = "This is a small circle.";
    private static final Vec3d OUTGOING_TARGET = new Vec3d(0, 6, 0);
    private static final DoubleFunction<Vec3d> OUTGOING = t -> spiral(t, OUTGOING_TARGET, 0.06, 6, 6);
    private static final DoubleFunction<Vec3d> SPHERE = t -> {
        double angle = t * Math.PI * 16;
        double radius = Math.sqrt(2) * Math.sin(t * Math.PI);
        return new Vec3d(Math.cos(angle) * radius, (t - 0.5) * 3, Math.sin(angle) * radius);
    };
    private FontRenderer font;

    void render(RiftJarBlockEntity jar, double x, double y, double z, float ticks, float outgoing) {
        if (jar.getConnections().isEmpty() && outgoing <= 0) return;

        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;
        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.translate(x, y, z);
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            if (outgoing > 0) draw(SPIRAL_TEXT, OUTGOING, -ticks * 0.002, outgoing, 0);
            for (RiftJarBlockEntity.Connection connection : jar.getConnections()) {
                Vec3d target = connection.getPosition().subtract(jar.getPos().getX() + 0.5,
                        jar.getPos().getY() + 0.6, jar.getPos().getZ() + 0.5);
                double strength = connection.getStrength();
                draw(SPIRAL_TEXT, t -> spiral(t, target, 0.1, 0.5, 8), -ticks * 0.002, strength, 0);
                draw(CIRCLE_TEXT, t -> SPHERE.apply(t).add(target), -ticks * 0.002, 1, 1 - strength);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        }
    }

    private static Vec3d spiral(double t, Vec3d target, double radius, double growth, int turns) {
        double angle = t * Math.PI * 2 * turns - Math.PI / 4;
        double distance = Math.sqrt(2) * radius * (1 + t * Math.abs(target.y) * growth);
        return target.scale(t).addVector(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    private void draw(String label, DoubleFunction<Vec3d> path, double shift, double cap, double hidden) {
        FontRenderer glyphs = getFont();
        Random mask = new Random(42);
        for (int index = 0; index < label.length(); index++) {
            char glyph = label.charAt(index);
            double progress = (double) index / label.length() + shift;
            progress -= Math.floor(progress);
            if (mask.nextFloat() < hidden || glyph == ' ' || progress > cap) continue;
            Vec3d point = path.apply(progress);
            Vec3d tangent = path.apply(Math.min(1, progress + 0.002)).subtract(path.apply(Math.max(0, progress - 0.002)));
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(point.x, point.y, point.z);
                GlStateManager.rotate((float) Math.toDegrees(-Math.atan2(tangent.z, tangent.x)), 0, 1, 0);
                GlStateManager.rotate((float) Math.toDegrees(Math.atan2(tangent.y, Math.hypot(tangent.x, tangent.z))), 0, 0, 1);
                GlStateManager.scale(1F / 60, -1F / 60, 1F / 60);
                glyphs.drawString(String.valueOf(glyph), 0, 0, 0xFFFFFFFF);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }

    private FontRenderer getFont() {
        if (font == null) {
            Minecraft client = Minecraft.getMinecraft();
            font = new FontRenderer(client.gameSettings, FONT, client.getTextureManager(), false);
            ((IReloadableResourceManager) client.getResourceManager()).registerReloadListener(font);
        }
        return font;
    }
}
