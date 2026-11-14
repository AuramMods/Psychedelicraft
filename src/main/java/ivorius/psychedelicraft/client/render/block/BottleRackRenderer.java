/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.BottleRackBlock;
import ivorius.psychedelicraft.block.entity.BottleRackBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public final class BottleRackRenderer extends TileEntitySpecialRenderer<BottleRackBlockEntity> {
    @Override
    public void render(BottleRackBlockEntity rack, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        IBlockState state = rack.getWorld().getBlockState(rack.getPos());
        if (!(state.getBlock() instanceof BottleRackBlock)) return;
        EnumFacing facing = state.getValue(BottleRackBlock.FACING);
        if (facing.getAxis() == EnumFacing.Axis.X) facing = facing.getOpposite();
        Random random = new Random(BlockRenderSeed.pack(rack.getPos()));
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            GlStateManager.rotate(facing.getHorizontalAngle() + 90, 0, 1, 0);
            GlStateManager.translate(0.14F, -0.55F, -0.8F);
            GlStateManager.rotate(90, 0, 0, 1);
            for (int slot = 0; slot < BottleRackBlockEntity.SIZE; slot++) {
                ItemStack bottle = rack.getBottles().getStackInSlot(slot);
                float tilt = (random.nextFloat() - 0.5F) * 4;
                if (bottle.isEmpty()) continue;
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate((1 - slot / 3) * 0.3F, 0, (slot % 3) * 0.3F);
                    GlStateManager.translate(0, 1, -1.2F);
                    GlStateManager.rotate(tilt, 1, 0, 0);
                    GlStateManager.translate(0, -1, 1.2F);
                    DrinkModels.render(bottle, rack.getPos());
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        } finally {
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }
}
