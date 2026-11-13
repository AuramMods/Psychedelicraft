/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import ivorius.psychedelicraft.client.render.DrugRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PlacedDrinksRenderer extends TileEntitySpecialRenderer<PlacedDrinkBlockEntity> {
    public static void registerModels() {
        DrinkModels.registerVariants();
    }

    @SubscribeEvent
    public static void loadGeometry(ModelBakeEvent event) {
        DrinkModels.loadGeometry(Minecraft.getMinecraft().getResourceManager());
    }

    @Override
    public void render(PlacedDrinkBlockEntity display, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        // Keep these local GL changes out of GlStateManager's state cache.
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_NORMALIZE);
        try {
            display.forEachDrink((offsetX, height, offsetZ, rotation, stack) -> {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(x + offsetX, y + height, z + offsetZ);
                    GlStateManager.rotate(rotation, 0, 1, 0);
                    GlStateManager.translate(-0.5, 0, -0.5);
                    return DrinkModels.render(stack, null);
                } finally {
                    GlStateManager.popMatrix();
                }
            });
        } finally {
            GL11.glPopAttrib();
        }
    }

    @SubscribeEvent
    public static void highlight(DrawBlockHighlightEvent event) {
        RayTraceResult target = event.getTarget();
        if (target.typeOfHit != RayTraceResult.Type.BLOCK) return;
        BlockPos pos = target.getBlockPos();
        EntityPlayer player = event.getPlayer();
        if (player.world.getBlockState(pos).getBlock() != PSBlocks.PLACED_DRINK) return;
        event.setCanceled(true);
        TileEntity tile = player.world.getTileEntity(pos);
        if (!(tile instanceof PlacedDrinkBlockEntity) || target.sideHit.getAxis() != EnumFacing.Axis.Y) return;
        PlacedDrinkBlockEntity display = (PlacedDrinkBlockEntity) tile;
        int cell = PlacedDrinkBlockEntity.cell((float) (target.hitVec.x - pos.getX()), (float) (target.hitVec.z - pos.getZ()));
        int existing = display.findColumn(cell);
        float partialTicks = event.getPartialTicks();
        double x = pos.getX() + PlacedDrinkBlockEntity.x(cell)
                - (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks);
        double y = pos.getY() - (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks);
        double z = pos.getZ() + PlacedDrinkBlockEntity.z(cell)
                - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);
        AxisAlignedBB outline = new AxisAlignedBB(x - 0.125, y, z - 0.125, x + 0.125,
                y + (existing < 0 ? 0.01 / 16 : 0.25), z + 0.125);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2);
            DrugRenderer.drawBlockOutline(outline, 0, 0, 0, 0.4F, target);
        } finally {
            GL11.glPopAttrib();
        }
    }
}
