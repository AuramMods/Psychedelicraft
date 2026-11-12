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

public final class BottleRackRenderer extends TileEntitySpecialRenderer<BottleRackBlockEntity> {
    @Override
    public void render(BottleRackBlockEntity rack, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        IBlockState state = rack.getWorld().getBlockState(rack.getPos());
        if (!(state.getBlock() instanceof BottleRackBlock)) return;
        EnumFacing facing = state.getValue(BottleRackBlock.FACING);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.rotate(180 - facing.getHorizontalAngle(), 0, 1, 0);
            for (int slot = 0; slot < BottleRackBlockEntity.SIZE; slot++) {
                ItemStack bottle = rack.getBottles().getStackInSlot(slot);
                if (bottle.isEmpty()) continue;
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate((1 - slot % 3) * 0.3, (2 - slot / 3) * 5 / 16D + 0.155, 0.27);
                    GlStateManager.rotate(-90, 1, 0, 0);
                    GlStateManager.translate(-0.5, 0, -0.5);
                    DrinkModels.render(bottle, rack.getPos());
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
