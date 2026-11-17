/* Copyright (c) 2014, Lukas Tenbrink. Native ray and emissive-center rendering adapted from Sollace. */
package ivorius.psychedelicraft.client.render.entity;
import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.ZeroMatter;
import ivorius.psychedelicraft.entity.RealityRiftEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.*;
import java.util.Random;
import java.lang.Math;
public final class RealityRiftRenderer extends EntityRenderer<RealityRiftEntity> {
    public static final ResourceLocation CENTER=Psychedelicraft.id("textures/entity/reality_rift/zero_center.png");
    public RealityRiftRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(RealityRiftEntity rift){return CENTER;}
    @Override public void render(RealityRiftEntity rift,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        float size=rift.getVisualSize(partial);if(size<=0)return;float scale=size<.01F?size*10:.1F+(size-.01F)*.1F,ticks=rift.tickCount+partial+rift.getInstability()*rift.getInstability()*3000;
        pose.pushPose();pose.translate(0,rift.getBbHeight()/2,0);pose.scale(scale,scale,scale);rays(pose,buffers.getBuffer(ZeroMatter.layer(ticks)),ticks);
        pose.pushPose();pose.scale(5,5,5);pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.translate(-.5,-.5,0);var vertices=buffers.getBuffer(RenderType.entityTranslucentEmissive(CENTER));
        center(vertices,pose,0,0,0,0);center(vertices,pose,1,0,1,0);center(vertices,pose,1,1,1,1);center(vertices,pose,0,1,0,1);pose.popPose();pose.popPose();super.render(rift,yaw,partial,pose,buffers,light);
    }
    private static void center(VertexConsumer out,PoseStack pose,float x,float y,float u,float v){out.vertex(pose.last().pose(),x,y,0).color(1F,1,1,1).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0).normal(pose.last().normal(),1,1,1).endVertex();}
    private static void rays(PoseStack pose,VertexConsumer out,float ticks){
        var random=new Random(432);pose.pushPose();float twist=ticks/200*Mth.HALF_PI*.5F;
        for(int i=0;i<20;i++){
            float wave=((float)i/20*28493+ticks)/10%20;if(wave>10)wave=20-wave;float alpha=1/(1+(float)Math.pow(2.71828F,-.8F*wave)*(1/.01F-1));if(alpha<=.01F)continue;
            pose.mulPose(new Quaternionf().rotateXYZ(random.nextFloat()*Mth.TWO_PI,random.nextFloat()*Mth.TWO_PI,random.nextFloat()*Mth.TWO_PI));pose.mulPose(new Quaternionf().rotateXYZ(random.nextFloat()*Mth.TWO_PI,random.nextFloat()*Mth.TWO_PI,random.nextFloat()*Mth.TWO_PI+twist));float length=random.nextFloat()*20+5,width=random.nextFloat()*2+1;var m=pose.last().pose();
            ray(out,m,0,0,0,alpha);ray(out,m,-2.5F*width,length,-.5F*width,0);ray(out,m,2.5F*width,length,-.5F*width,0);ray(out,m,0,length,width,0);ray(out,m,-2.5F*width,length,-.5F*width,0);
        }
        pose.popPose();
    }
    private static void ray(VertexConsumer out,Matrix4f matrix,float x,float y,float z,float alpha){out.vertex(matrix,x,y,z).color(1F,1,1,alpha).endVertex();}
}
