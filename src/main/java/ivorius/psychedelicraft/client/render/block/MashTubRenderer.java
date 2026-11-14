/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.TankFluidAppearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class MashTubRenderer extends TileEntitySpecialRenderer<MashTubBlockEntity> {
    @Override
    public void render(MashTubBlockEntity vat, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        BlockPos anchor = vat.getPos();
        FluidStack fluid = vat.getTank().getFluid();
        float height = 0.1F;
        if (fluid != null) {
            height = vat.getFluidSurface();
            TankFluidAppearance appearance = TankFluidAppearance.of(fluid);
            drawSurface(appearance.sprite, appearance.color, x, y + height, z, anchor);
        } else if (!vat.getSolidOutput().isEmpty()) {
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher()
                    .getModelForState(Blocks.DIRT.getDefaultState()).getParticleTexture();
            drawSurface(sprite, 0xffffffff, x, y + 0.5, z, anchor);
        }
        if (vat.getSuppliedIngredients().isEmpty()) return;
        Minecraft client = Minecraft.getMinecraft();
        float ticks = (client.player == null ? 0 : client.player.ticksExisted) + partialTicks;
        Random random = new Random(ingredientSeed(anchor));
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : vat.getSuppliedIngredients()) {
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        for (Map.Entry<Item, Integer> ingredient : counts.entrySet()) {
            ItemStack stack = ingredient.getKey().getDefaultInstance();
            for (int copy = 0; copy < ingredient.getValue(); copy++) {
                float dx = 0.5F + (random.nextFloat() - 0.5F) * 1.5F;
                float dz = 0.5F + (random.nextFloat() - 0.5F) * 1.5F;
                float rotation = random.nextFloat() * 360F;
                float phase = (ticks + copy * 5) / 8F;
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(x + dx, y + 0.75F + height / 16F - 0.02F, z + dz);
                    GlStateManager.rotate(rotation, 0, 1, 0);
                    GlStateManager.scale(0.3F, 0.3F, 0.3F);
                    GlStateManager.translate(0, 0.2F * MathHelper.sin(phase), -0.2F);
                    GlStateManager.rotate(6F * MathHelper.cos(phase), 0, 0, 1);
                    GlStateManager.rotate(-(ticks + copy) % 360F, 0, 1, 0);
                    DrugRenderer.renderBlockItem(client.getRenderItem(), stack, anchor);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private static long ingredientSeed(BlockPos pos) {
        // The donor packs Z between X and Y. Forge 1.12 packs Y between X and Z.
        long packed = ((long) pos.getX() & 0x3ffffffL) << 38
                | ((long) pos.getZ() & 0x3ffffffL) << 12
                | ((long) pos.getY() & 0xfffL);
        return packed + 1;
    }

    private void drawSurface(TextureAtlasSprite sprite, int color, double x, double y, double z, BlockPos anchor) {
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255, alpha = color >>> 24;
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int ix = 0; ix < 2; ix++) {
            for (int iz = 0; iz < 2; iz++) {
                double left = x - 0.4375 + ix * 0.9375;
                double front = z - 0.4375 + iz * 0.9375;
                double right = left + 0.9375;
                double back = front + 0.9375;
                buffer.pos(left, y, front).tex(sprite.getMinU(), sprite.getMinV()).color(red, green, blue, alpha).endVertex();
                buffer.pos(left, y, back).tex(sprite.getMinU(), sprite.getMaxV()).color(red, green, blue, alpha).endVertex();
                buffer.pos(right, y, back).tex(sprite.getMaxU(), sprite.getMaxV()).color(red, green, blue, alpha).endVertex();
                buffer.pos(right, y, front).tex(sprite.getMaxU(), sprite.getMinV()).color(red, green, blue, alpha).endVertex();
            }
        }
        DrugRenderer.drawWorldQuads(tessellator, anchor);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
    }
}
