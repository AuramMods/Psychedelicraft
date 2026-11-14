/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.TankFluidAppearance;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

public final class FlaskRenderer extends TileEntitySpecialRenderer<FluidMachineBlockEntity> {
    private static final double UNIT = 0.124;

    @Override
    public void render(FluidMachineBlockEntity machine, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        FluidStack contents = machine.getTank().getFluid();
        if (contents == null) return;
        TankFluidAppearance appearance = TankFluidAppearance.of(contents);
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();
        try {
            GlStateManager.translate(x + 0.5, y + 0.002, z + 0.5);
            GlStateManager.scale(UNIT, UNIT, UNIT);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            float fill = MathHelper.clamp((float) contents.amount / machine.getTank().getCapacity(), 0, 1);
            chamber(buffer, appearance.sprite, appearance.color, 0, fill * 2, 2);
            if (fill > 0.5F) {
                double neck = machine instanceof DistilleryBlockEntity ? 4.5 : 3.5;
                chamber(buffer, appearance.sprite, appearance.color, neck, (fill - 0.5F) * 2, 1.5);
            }
            DrugRenderer.drawWorldQuads(tessellator, machine.getPos());
        } finally {
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    private static void chamber(BufferBuilder buffer, TextureAtlasSprite sprite, int color,
                                double bottom, double height, double edge) {
        double top = bottom + height;
        top(buffer, sprite, color, -1, 1, -1, 1, top);
        top(buffer, sprite, color, -1, 1, -edge, -1, top);
        top(buffer, sprite, color, -1, 1, 1, edge, top);
        top(buffer, sprite, color, -edge, -1, -1, 1, top);
        top(buffer, sprite, color, 1, edge, -1, 1, top);
        vertex(buffer, sprite, color, -1, bottom, -edge, 0, 0);
        vertex(buffer, sprite, color, -1, top, -edge, 0, 16);
        vertex(buffer, sprite, color, 1, top, -edge, 16, 16);
        vertex(buffer, sprite, color, 1, bottom, -edge, 16, 0);

        vertex(buffer, sprite, color, -1, bottom, edge, 0, 0);
        vertex(buffer, sprite, color, 1, bottom, edge, 16, 0);
        vertex(buffer, sprite, color, 1, top, edge, 16, 16);
        vertex(buffer, sprite, color, -1, top, edge, 0, 16);

        vertex(buffer, sprite, color, -edge, bottom, -1, 0, 0);
        vertex(buffer, sprite, color, -edge, bottom, 1, 16, 0);
        vertex(buffer, sprite, color, -edge, top, 1, 16, 16);
        vertex(buffer, sprite, color, -edge, top, -1, 0, 16);

        vertex(buffer, sprite, color, edge, bottom, -1, 0, 0);
        vertex(buffer, sprite, color, edge, top, -1, 16, 0);
        vertex(buffer, sprite, color, edge, top, 1, 16, 16);
        vertex(buffer, sprite, color, edge, bottom, 1, 0, 16);
    }

    private static void top(BufferBuilder buffer, TextureAtlasSprite sprite, int color, double left, double right,
                            double front, double back, double y) {
        vertex(buffer, sprite, color, left, y, front, 0, 0);
        vertex(buffer, sprite, color, left, y, back, 0, 16);
        vertex(buffer, sprite, color, right, y, back, 16, 16);
        vertex(buffer, sprite, color, right, y, front, 16, 0);
    }

    private static void vertex(BufferBuilder buffer, TextureAtlasSprite sprite, int color,
                               double x, double y, double z, int u, int v) {
        buffer.pos(x, y, z).tex(sprite.getInterpolatedU(u), sprite.getInterpolatedV(v))
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }
}
