/* Copyright (c) 2014, Lukas Tenbrink. Native renderer by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.block.BottleRackBlock;
import ivorius.psychedelicraft.block.entity.BottleRackBlockEntity;
import ivorius.psychedelicraft.client.render.PlacedDrinkModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import java.util.Random;
public final class BottleRackRenderer implements BlockEntityRenderer<BottleRackBlockEntity> {
    public BottleRackRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(BottleRackBlockEntity rack,float partial,PoseStack pose,MultiBufferSource vertices,int light,int overlay){
        Direction facing=rack.getBlockState().getValue(BottleRackBlock.FACING);if(facing.getAxis()==Direction.Axis.X)facing=facing.getOpposite();Random random=new Random(rack.getBlockPos().asLong());
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot()+90));pose.translate(.14,-.55,-.8);pose.mulPose(Axis.ZP.rotationDegrees(90));
        for(int slot=0;slot<BottleRackBlockEntity.SIZE;slot++){var stack=rack.getItem(slot);float tilt=(random.nextFloat()-.5F)*4;if(stack.isEmpty())continue;
            pose.pushPose();pose.translate((1-slot/3)*.3,0,(slot%3)*.3);pose.translate(0,1,-1.2);pose.mulPose(Axis.XP.rotationDegrees(tilt));pose.translate(0,-1,1.2);PlacedDrinkModels.render(stack,rack.getBlockPos(),pose,vertices,light,overlay);pose.popPose();
        }pose.popPose();
    }
}
