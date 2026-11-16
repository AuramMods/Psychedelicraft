/* Copyright (c) 2014, Lukas Tenbrink. Native model and renderer by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
public final class BarrelRenderer implements BlockEntityRenderer<BarrelBlockEntity>{
    private final BarrelModel model=new BarrelModel(BarrelModel.getLayerDefinition().bakeRoot());
    public BarrelRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(BarrelBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();
        try{
            pose.translate(.5,0,.5);var state=entity.getBlockState();var facing=state.getValue(BarrelBlock.FACING);pose.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));
            var texture=Psychedelicraft.id("textures/entity/barrel/"+ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath()+".png");model.animate(entity,partial);model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,overlay);
            var fluid=entity.getTank().getFluid();if(fluid.isEmpty())return;var profile=PSFluids.profile(fluid.getFluid());
            ResourceLocation symbol=profile==null?new ResourceLocation(PSFluids.id(fluid.getFluid())):profile.getSymbol(fluid);
            var icon=new ResourceLocation(symbol.getNamespace(),"textures/"+(profile==null?"fluid/":"")+symbol.getPath()+".png");
            if(Minecraft.getInstance().getResourceManager().getResource(icon).isEmpty())return;
            pose.translate(0,.5,0);if(facing.getAxis().isVertical()){pose.mulPose(Axis.XP.rotationDegrees(90));pose.translate(0,-.1,0);}
            var buffer=buffers.getBuffer(RenderType.entityCutoutNoCull(icon));float side=.4376F-.06F;
            face(pose,buffer,-side,-.5F,.5F,-side,.5F,.5F,-side,.5F,-.5F,-side,-.5F,-.5F,light,overlay,-1);
            face(pose,buffer,side,-.5F,-.5F,side,.5F,-.5F,side,.5F,.5F,side,-.5F,.5F,light,overlay,1);
        }finally{pose.popPose();}
    }
    private static void face(PoseStack pose,VertexConsumer buffer,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,float x4,float y4,float z4,int light,int overlay,int normal){
        vertex(pose,buffer,x1,y1,z1,1,1,light,overlay,normal);vertex(pose,buffer,x2,y2,z2,1,0,light,overlay,normal);vertex(pose,buffer,x3,y3,z3,0,0,light,overlay,normal);vertex(pose,buffer,x4,y4,z4,0,1,light,overlay,normal);
    }
    private static void vertex(PoseStack pose,VertexConsumer buffer,float x,float y,float z,float u,float v,int light,int overlay,int normal){buffer.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),normal,0,0).endVertex();}
}
