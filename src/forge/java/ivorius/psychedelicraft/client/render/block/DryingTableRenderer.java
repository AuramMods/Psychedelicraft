/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 5 Jan 2023.
 */

package ivorius.psychedelicraft.client.render.block;

import java.util.Random;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;

public final class DryingTableRenderer extends TileEntitySpecialRenderer<DryingTableBlockEntity> {
    @Override
    public void render(DryingTableBlockEntity table, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        Random random = new Random(table.getPos().toLong() + 1);
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            ItemStack stack = table.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            boolean output = slot == 0;
            float dx = output ? 0.5F : 0.35F + random.nextFloat() * 0.3F;
            float dz = output ? 0.5F : 0.35F + random.nextFloat() * 0.3F;
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x + dx, y + 0.75F + slot / 500F, z + dz);
                GlStateManager.rotate(random.nextFloat() * 360F, 0, 1, 0);
                float scale = output ? 0.75F : 0.5F;
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(0, 0, -0.2F);
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.rotate(50, 0, 0, 1);
                Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }
}
