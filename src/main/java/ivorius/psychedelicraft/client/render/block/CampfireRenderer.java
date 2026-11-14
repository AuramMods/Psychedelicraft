package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.CampfireBlock;
import ivorius.psychedelicraft.block.entity.CampfireBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public final class CampfireRenderer extends TileEntitySpecialRenderer<CampfireBlockEntity> {
    @Override
    public void render(CampfireBlockEntity fire, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = fire.getWorld().getBlockState(fire.getPos());
        if (!(state.getBlock() instanceof CampfireBlock)) return;
        int facing = state.getValue(CampfireBlock.FACING).getHorizontalIndex();
        for (int slot = 0; slot < CampfireBlockEntity.SLOT_COUNT; slot++) {
            ItemStack food = fire.getFood(slot);
            if (food.isEmpty()) continue;
            EnumFacing direction = EnumFacing.getHorizontal(facing + slot);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x + 0.5D, y + 0.44921875D, z + 0.5D);
                GlStateManager.rotate(-direction.getHorizontalAngle(), 0, 1, 0);
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.translate(-0.3125F, -0.3125F, 0);
                GlStateManager.scale(0.375F, 0.375F, 0.375F);
                DrugRenderer.renderBlockItem(Minecraft.getMinecraft().getRenderItem(), food, fire.getPos());
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }
}
