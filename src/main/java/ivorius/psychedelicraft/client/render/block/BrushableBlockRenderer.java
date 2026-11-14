package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.block.SuspiciousSandBlock;
import ivorius.psychedelicraft.block.entity.BrushableBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public final class BrushableBlockRenderer extends TileEntitySpecialRenderer<BrushableBlockEntity> {
    @Override
    public void render(BrushableBlockEntity tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        ItemStack stack = tile.getExposedItem();
        EnumFacing face = tile.getBrushFace();
        if (stack.isEmpty() || face == null || !tile.hasWorld()) return;
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof SuspiciousSandBlock)) return;
        int dust = state.getValue(SuspiciousSandBlock.DUSTED);
        if (dust == 0) return;
        double offset = .24D + dust * .075D;
        float oldLightX = OpenGlHelper.lastBrightnessX;
        float oldLightY = OpenGlHelper.lastBrightnessY;
        int light = tile.getWorld().getCombinedLight(tile.getPos().offset(face), 0);
        GlStateManager.pushMatrix();
        RenderHelper.enableStandardItemLighting();
        try {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
            GlStateManager.translate(x + .5 + offset * face.getFrontOffsetX(), y + .5 + offset * face.getFrontOffsetY(),
                    z + .5 + offset * face.getFrontOffsetZ());
            GlStateManager.rotate(face.getAxis() == EnumFacing.Axis.X ? 176 : 86, 0, 1, 0);
            GlStateManager.scale(.5F, .5F, .5F);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldLightX, oldLightY);
            GlStateManager.popMatrix();
        }
    }
}
