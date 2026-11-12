package ivorius.psychedelicraft.client.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class RiftRenderState implements AutoCloseable {
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

    public RiftRenderState() {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
    }

    @Override
    public void close() {
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.depthMask(depthMask);
        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
    }
}
