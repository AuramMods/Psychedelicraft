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
                Minecraft client = Minecraft.getMinecraft();
                int age = (int) (System.currentTimeMillis() % 500 / 100);
                float time = client.world == null ? 0F
                        : gameTime(client.world.getTotalWorldTime(), client.getRenderPartialTicks());
                renderJar(0.5, 0, 0.5, EnumFacing.NORTH, RiftJarItem.getRiftFraction(stack), 0, 0, age, age, time);
            }
        };
    }

    @Override
    public void render(RiftJarBlockEntity jar, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        IBlockState state = jar.getWorld().getBlockState(jar.getPos());
        if (state.getBlock() != PSBlocks.RIFT_JAR) return;
        int age = jar.getVisualAge();
        float ticks = age + partialTicks;
        float time = gameTime(jar.getWorld().getTotalWorldTime(), partialTicks);
        float lid = jar.getLid(1F);
        float handle = jar.getHandle(1F);
        renderJar(x + 0.5, y, z + 0.5, state.getValue(RiftJarBlock.FACING), jar.getAmount(), lid, handle, age, ticks, time);
        glyphs.render(jar, x + 0.5, y + 0.5, z + 0.5, ticks, handle * lid);
    }

    private static float gameTime(long tick, float partialTicks) {
        return ((tick % 24000) + partialTicks) / 24000F;
    }

    private void renderJar(double x, double y, double z, EnumFacing facing, float amount,
                           float lid, float handle, int age, float ticks, float time) {
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(90 - facing.getHorizontalAngle(), 0, 1, 0);
            GlStateManager.disableCull();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(0, 1.501, 0);
                GlStateManager.rotate(180, 1, 0, 0);
                model.cork.rotationPointX = lid * 2;
                model.cork.rotateAngleY = lid * 0.1F;
                model.knot.rotateAngleZ = 0.2602503F + handle * (1 + MathHelper.sin(age * 0.1F) * 0.1F) * 0.5F;

                Minecraft.getMinecraft().getTextureManager().bindTexture(GLASS);
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.1F);
                GlStateManager.color(1, 1, 1, 1);
                try (Scope scope = DrugRenderer.beginWorldModel()) {
                    model.render();
                    if (amount > 0.5F) {
                        float crackAlpha = Math.min((amount - 0.5F) * 2, 1);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(CRACKED);
                        // The donor tests the texture alpha before applying the crack fade.
                        GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.1F * crackAlpha);
                        GlStateManager.color(1, 1, 1, crackAlpha);
                        model.render();
                    }
                }
            } finally {
                GlStateManager.popMatrix();
            }

            if (amount > 0) {
                GlStateManager.disableAlpha();
                GlStateManager.translate(0, 0.001F, 0);
                GlStateManager.scale(0.9F, 1, 0.9F);
                ZeroMatterRenderer.INSTANCE.draw(ticks, time, () -> drawInterior(Math.min(amount * 2, 1)));
            }
        } finally {
            GlStateManager.alphaFunc(alphaFunc, alphaRef);
            GlStateManager.depthFunc(depthFunc);
        }
    }

    private static void drawInterior(float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        interiorSection(buffer, 4, 0, 5, 0.001F, alpha);
        interiorSection(buffer, 3, 5, 7, -0.001F, alpha);
        interiorSection(buffer, 4, 7, 9, 0.001F, alpha);
        tessellator.draw();
    }

    private static void interiorSection(BufferBuilder buffer, int halfWidth, int minY, int maxY, float dilationY, float alpha) {
        float width = (halfWidth + 0.001F) / 16;
        float bottom = (minY - dilationY) / 16;
        float top = (maxY + dilationY) / 16;
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
            {2, 3, 0, 1}, {5, 4, 7, 6}, {0, 3, 7, 4}, {1, 0, 4, 5}, {2, 1, 5, 6}, {3, 2, 6, 7}
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
