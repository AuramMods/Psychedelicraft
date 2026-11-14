package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.BeeEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public final class BeeRenderer extends Render<BeeEntity> {
    public BeeRenderer(RenderManager manager) {
        super(manager);
        shadowSize = 0.35F;
    }

    @Override
    public void doRender(BeeEntity bee, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        float scale = bee.isChild() ? 0.5F : 1F;
        GlStateManager.translate(x, y - scale * 1.5D / 16D, z);
        GlStateManager.rotate(180F - yaw, 0F, 1F, 0F);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.disableTexture2D();
        if (renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(getTeamColor(bee));
        }
        try {
            ApparitionModels.drawBee(bee.ticksExisted + partialTicks, bee.hasNectar());
        } finally {
            if (renderOutlines) {
                GlStateManager.disableOutlineMode();
                GlStateManager.disableColorMaterial();
            }
            GlStateManager.enableTexture2D();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }
        super.doRender(bee, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(BeeEntity bee) {
        return null;
    }
}
