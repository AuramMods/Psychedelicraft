/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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
        double x = pos.getX() + PlacedDrinkBlockEntity.x(existing < 0 ? cell : existing)
                - (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks);
        double y = pos.getY() - (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks);
        double z = pos.getZ() + PlacedDrinkBlockEntity.z(existing < 0 ? cell : existing)
                - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2);
        try {
            RenderGlobal.drawBoundingBox(x - 0.125, y + 0.001, z - 0.125, x + 0.125,
                    y + (existing < 0 ? 0.002 : 0.25), z + 0.125, 0, 0, 0, 0.4F);
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }
}
