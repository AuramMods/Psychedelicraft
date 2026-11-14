/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.shader;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

final class MotionBlurHistory {
    private static final int SAMPLE_COUNT = 30;
    private static final float SAMPLE_INTERVAL = 0.5F;
    private final Framebuffer[] samples = new Framebuffer[SAMPLE_COUNT];
    private final float[] sampleTimes = new float[SAMPLE_COUNT];
    private int latest = -1;
    private int width;
    private int height;
    private int sourceWidth;
    private int sourceHeight;
    private float lastCapture;

    void render(Framebuffer source, int width, int height, float ticks, float strength, FrameRenderer renderer) throws IOException {
        if (this.width != width || this.height != height || sourceWidth != source.framebufferWidth
                || sourceHeight != source.framebufferHeight || ticks < lastCapture) {
            release();
            this.width = width;
            this.height = height;
            sourceWidth = source.framebufferWidth;
            sourceHeight = source.framebufferHeight;
        }
        if (latest < 0) {
            capture(ticks, renderer);
        } else {
            int intervals = MathHelper.floor((ticks - lastCapture) / SAMPLE_INTERVAL);
            // The donor samples the current frame for every elapsed half-tick in the history window.
            float finalCapture = lastCapture + intervals * SAMPLE_INTERVAL;
            for (int offset = Math.min(intervals, SAMPLE_COUNT) - 1; offset >= 0; offset--) {
                capture(finalCapture - offset * SAMPLE_INTERVAL, renderer);
            }
        }

        // Draw the ring in the donor's slot order and skip samples older than its history window.
        for (int offset = 0; offset < samples.length; offset++) {
            int index = (latest + offset) % samples.length;
            if (samples[index] != null && ticks - sampleTimes[index] < SAMPLE_COUNT * SAMPLE_INTERVAL) {
                renderer.draw(samples[index], Math.min(1, (index + 1) * 0.002F * strength));
            }
        }
    }

    private void capture(float time, FrameRenderer renderer) throws IOException {
        int index = (latest + 1) % samples.length;
        if (samples[index] == null) samples[index] = new Framebuffer(width, height, false);
        renderer.draw(samples[index], 0);
        sampleTimes[index] = time;
        latest = index;
        lastCapture = time;
    }

    void release() {
        if (latest < 0 && samples[0] == null) return;
        int framebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        for (int index = 0; index < samples.length; index++) {
            Framebuffer sample = samples[index];
            if (sample == null) continue;
            if (framebuffer == sample.framebufferObject) framebuffer = 0;
            if (texture == sample.framebufferTexture) texture = 0;
            TextureUtil.deleteTexture(sample.framebufferTexture);
            OpenGlHelper.glDeleteFramebuffers(sample.framebufferObject);
            samples[index] = null;
        }
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
        GlStateManager.bindTexture(texture);
        latest = -1;
        lastCapture = 0;
    }

    @FunctionalInterface
    interface FrameRenderer {
        void draw(Framebuffer sample, float opacity) throws IOException;
    }
}
