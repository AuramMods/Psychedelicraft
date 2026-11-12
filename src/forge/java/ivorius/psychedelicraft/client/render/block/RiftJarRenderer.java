/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.RiftJarBlock;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.TranslucentRenderState;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import ivorius.psychedelicraft.client.render.shader.ZeroMatterRenderer;
import ivorius.psychedelicraft.item.RiftJarItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public final class RiftJarRenderer extends TileEntitySpecialRenderer<RiftJarBlockEntity> {
    private static final ResourceLocation GLASS = Psychedelicraft.id("textures/entity/rift_jar/rift_jar.png");
    private static final ResourceLocation CRACKED = Psychedelicraft.id("textures/entity/rift_jar/rift_jar_cracked.png");
    private final JarModel model = new JarModel();
    private final RiftJarGlyphs glyphs = new RiftJarGlyphs();

    public TileEntityItemStackRenderer itemRenderer() {
        return new TileEntityItemStackRenderer() {
            @Override
            public void renderByItem(ItemStack stack, float partialTicks) {
                float ticks = (System.currentTimeMillis() % 24000) / 50F;
                renderJar(0.5, 0, 0.5, EnumFacing.NORTH, RiftJarItem.getRiftFraction(stack), 0, 0, ticks, ticks / 24000);
            }
        };
    }

    @Override
    public void render(RiftJarBlockEntity jar, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        IBlockState state = jar.getWorld().getBlockState(jar.getPos());
        if (state.getBlock() != PSBlocks.RIFT_JAR) return;
        float ticks = jar.getVisualAge() + partialTicks;
        float time = ((jar.getWorld().getTotalWorldTime() % 24000) + partialTicks) / 24000F;
        float lid = jar.getLid(partialTicks);
        float handle = jar.getHandle(partialTicks);
        renderJar(x + 0.5, y, z + 0.5, state.getValue(RiftJarBlock.FACING), jar.getAmount(), lid, handle, ticks, time);
        glyphs.render(jar, x + 0.5, y + 0.6, z + 0.5, ticks, handle * lid);
    }

    private void renderJar(double x, double y, double z, EnumFacing facing, float amount,
                           float lid, float handle, float ticks, float time) {
        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(90 - facing.getHorizontalAngle(), 0, 1, 0);
            if (amount > 0) {
                GlStateManager.disableCull();
                ZeroMatterRenderer.INSTANCE.draw(ticks, time, () -> drawInterior(Math.min(amount * 2, 1)));
            }

            GlStateManager.enableCull();
            GlStateManager.translate(0, 1.501, 0);
            GlStateManager.rotate(180, 1, 0, 0);
            model.cork.rotationPointX = lid * 2;
            model.cork.rotateAngleY = lid * 0.1F;
            model.knot.rotateAngleZ = 0.2602503F + handle * (1 + MathHelper.sin(ticks * 0.1F) * 0.1F) * 0.5F;

            Minecraft.getMinecraft().getTextureManager().bindTexture(GLASS);
            GlStateManager.color(1, 1, 1, 1);
            try (Scope scope = DrugRenderer.beginWorldModel()) {
                model.render();
                if (amount > 0.5F) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(CRACKED);
                    GlStateManager.color(1, 1, 1, Math.min((amount - 0.5F) * 2, 1));
                    model.render();
                }
            }
        }
    }

    private static void drawInterior(float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        interiorSection(buffer, 3.6 / 16, 0.001, 5D / 16, alpha);
        interiorSection(buffer, 2.7 / 16, 5D / 16, 7D / 16, alpha);
        interiorSection(buffer, 3.6 / 16, 7D / 16, 9D / 16, alpha);
        tessellator.draw();
    }

    private static void interiorSection(BufferBuilder buffer, double width, double bottom, double top, float alpha) {
        double[][] corners = {
                {-width, bottom, -width}, {width, bottom, -width}, {width, bottom, width}, {-width, bottom, width},
                {-width, top, -width}, {width, top, -width}, {width, top, width}, {-width, top, width}
        };
        for (int[] face : INTERIOR_FACES) {
            for (int i = 0; i < face.length; i++) {
                double[] point = corners[face[i]];
                buffer.pos(point[0], point[1], point[2]).tex(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0)
                        .color(1F, 1F, 1F, alpha).endVertex();
            }
        }
    }

    private static final int[][] INTERIOR_FACES = {
            {0, 1, 2, 3}, {0, 4, 5, 1}, {1, 5, 6, 2}, {2, 6, 7, 3}, {3, 7, 4, 0}, {4, 7, 6, 5}
    };

    private static final class JarModel extends ModelBase {
        private final ModelRenderer cork;
        private final ModelRenderer knot;
        private final ModelRenderer[] parts;

        private JarModel() {
            textureWidth = 64;
            textureHeight = 32;
            cork = part(33, 16, 0, 10, -3, -0.001F, -3, 6, 2, 6, 0);
            knot = part(33, 2, -3.5F, 17, 0, 0, -4, 0, 5, 8, 0.001F);
            parts = new ModelRenderer[]{
                    part(0, 0, 0, 19, -4, 0, -4, 8, 5, 8, 0),
                    part(0, 14, 0, 12, -4, 0, -4, 8, 5, 8, 0),
                    part(33, 24, 0, 17, -3, 0, -3, 6, 2, 6, 0),
                    part(33, 0, 0, 17, -3.5F, 0, -3.5F, 7, 2, 7, 0),
                    knot, cork
            };
        }

        private ModelRenderer part(int u, int v, float pivotX, float pivotY, float x, float y, float z,
                                   int width, int height, int depth, float dilation) {
            ModelRenderer part = new ModelRenderer(this, u, v);
            part.mirror = true;
            part.setRotationPoint(pivotX, pivotY, 0);
            part.addBox(x, y, z, width, height, depth, dilation);
            return part;
        }

        private void render() {
            for (ModelRenderer part : parts) part.render(1F / 16);
        }
    }
}
