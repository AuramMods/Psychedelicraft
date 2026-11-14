package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.ConduitBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public final class ConduitRenderer extends TileEntitySpecialRenderer<ConduitBlockEntity> {
    private static final ResourceLocation BASE = texture("base");
    private static final ResourceLocation CAGE = texture("cage");
    private static final ResourceLocation WIND = texture("wind");
    private static final ResourceLocation VERTICAL_WIND = texture("wind_vertical");
    private static final ResourceLocation OPEN = texture("open_eye");
    private static final ResourceLocation CLOSED = texture("closed_eye");
    private static final float UNIT = 1F / 16;
    private static final int WIND_FRAMES = 22;
    private final ModelRenderer base = box(32, 16, 6, 6, 0F);
    private final ModelRenderer cage = box(32, 16, 8, 8, 0F);
    private final ModelRenderer wind = box(64, WIND_FRAMES * 32, 16, 16, 0F);
    private final ModelRenderer eye = box(16, 16, 8, 0, .01F);

    private static ResourceLocation texture(String id) {
        return Psychedelicraft.id("textures/entity/conduit/" + id + ".png");
    }

    private static ModelRenderer box(int texWidth, int texHeight, int width, int depth, float inflation) {
        ModelBase model = new ModelBase() {};
        model.textureWidth = texWidth;
        model.textureHeight = texHeight;
        ModelRenderer mesh = new ModelRenderer(model, 0, 0);
        mesh.addBox(-width / 2F, -width / 2F, -depth / 2F, width, width, depth, inflation);
        return mesh;
    }

    public TileEntityItemStackRenderer itemRenderer() {
        return new TileEntityItemStackRenderer() {
            @Override
            public void renderByItem(ItemStack stack, float partialTicks) {
                renderShell(.5, .5, .5, 0);
            }
        };
    }

    @Override
    public void render(ConduitBlockEntity tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        try (Scope scope = DrugRenderer.beginWorldModel()) {
            if (!tile.isActive()) {
                renderShell(x + .5, y + .5, z + .5, tile.rotation(0));
                return;
            }
            renderActive(tile, x + .5, y, z + .5, partialTicks);
        }
    }

    private void renderShell(double x, double y, double z, float degrees) {
        GlStateManager.pushMatrix();
        bindTexture(BASE);
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(degrees, 0, 1, 0);
        base.render(UNIT);
        GlStateManager.popMatrix();
    }

    private void renderActive(ConduitBlockEntity tile, double x, double y, double z, float partialTicks) {
        boolean culled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableCull();
        float bob = MathHelper.sin((tile.ticks() + partialTicks) * .1F) / 2F + .5F;
        double height = y + .3 + (bob * bob + bob) * .2;
        GlStateManager.pushMatrix();
        bindTexture(CAGE);
        GlStateManager.translate(x, height, z);
        GlStateManager.rotate(tile.rotation(partialTicks) * (180F / (float) Math.PI), .5F, 1F, .5F);
        cage.render(UNIT);
        GlStateManager.popMatrix();

        renderWind(x, y + .5, z, tile.ticks());

        GlStateManager.pushMatrix();
        bindTexture(tile.isHunting() ? OPEN : CLOSED);
        GlStateManager.translate(x, height, z);
        GlStateManager.rotate(-rendererDispatcher.entityYaw, 0, 1, 0);
        GlStateManager.rotate(rendererDispatcher.entityPitch, 1, 0, 0);
        GlStateManager.rotate(180F, 0, 0, 1);
        GlStateManager.scale(2F / 3, 2F / 3, 2F / 3);
        eye.render(UNIT);
        GlStateManager.popMatrix();
        if (culled) GlStateManager.enableCull();
    }

    private void renderWind(double x, double y, double z, int ticks) {
        int orientation = ticks / 66 % 3;
        bindTexture(orientation == 1 ? VERTICAL_WIND : WIND);
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, (ticks / 3 % WIND_FRAMES) / (float) WIND_FRAMES, 0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        if (orientation == 1) GlStateManager.rotate(90F, 1, 0, 0);
        if (orientation == 2) GlStateManager.rotate(90F, 0, 0, 1);
        wind.render(UNIT);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.scale(.875F, .875F, .875F);
        GlStateManager.rotate(180F, 1, 0, 0);
        GlStateManager.rotate(180F, 0, 0, 1);
        wind.render(UNIT);
        GlStateManager.popMatrix();

        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }
}
