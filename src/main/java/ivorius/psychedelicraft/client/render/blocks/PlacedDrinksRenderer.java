/* Copyright (c) 2014, Lukas Tenbrink. Native renderer by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import ivorius.psychedelicraft.client.render.PlacedDrinkModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
public final class PlacedDrinksRenderer implements BlockEntityRenderer<PlacedDrinkBlockEntity> {
    public PlacedDrinksRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(PlacedDrinkBlockEntity display,float partial,PoseStack pose,MultiBufferSource vertices,int light,int overlay){
        display.forEachDrink((x,y,z,rotation,stack)->{
            pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotationDegrees(rotation));pose.translate(-.5,0,-.5);
            PlacedDrinkModels.render(stack,display.getBlockPos(),pose,vertices,light,overlay);pose.popPose();return PlacedDrinkModels.geometry(stack).height();
        });
    }
}
