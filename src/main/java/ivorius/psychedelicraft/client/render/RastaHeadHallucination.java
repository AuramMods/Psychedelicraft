/* Copyright (c) 2014, Lukas Tenbrink. Modern model originally adapted by Sollace. */
package ivorius.psychedelicraft.client.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.shader.PostRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;

final class RastaHeadHallucination {
    private static final ResourceLocation TEXTURE=Psychedelicraft.id("textures/drug/cannabis/rasta_head_hallucination.png");
    private static final ModelPart MODEL=RastaHeadModel.create();
    private final LocalPlayer owner;
    private final Pig head;
    final RastaHeadChat chat;
    private final int maxAge;
    private final float scale,distance,planeRotationY,planeRotationZ;
    private Vec3 previousPosition;
    private int age;
    RastaHeadHallucination(LocalPlayer owner,Vec3 position){
        this.owner=owner;head=EntityType.PIG.create(owner.level());head.setPos(position);previousPosition=position;chat=new RastaHeadChat(owner);
        var random=owner.getRandom();maxAge=(random.nextInt(59)+120)*20;scale=1+random.nextFloat()/2;distance=2+random.nextFloat()*5;
        planeRotationY=random.nextFloat()*Mth.HALF_PI;planeRotationZ=random.nextFloat()*Mth.HALF_PI;
    }
    boolean tick(boolean visible){
        head.tickCount=++age;chat.tick(visible);previousPosition=head.position();head.yHeadRotO=head.yHeadRot;head.xRotO=head.getXRot();
        Vec3 eyes=owner.getEyePosition();lookAt(eyes);
        int seed=owner.tickCount+head.getId()*3;
        Vec3 offset=new Vec3(Mth.sin(seed/50F)*distance,Mth.sin(seed/10F)+head.getId()%5-1,Mth.cos(seed/50F)*distance).yRot(planeRotationY);
        float sin=Mth.sin(planeRotationZ),cos=Mth.cos(planeRotationZ);offset=new Vec3(offset.x*cos+offset.y*sin,offset.y*cos-offset.x*sin,offset.z);
        Vec3 delta=eyes.add(offset).subtract(previousPosition);double length=delta.length();
        Vec3 velocity=length<1E-4?Vec3.ZERO:delta.normalize().scale(Math.log((float)length));head.setDeltaMovement(velocity);head.setPos(previousPosition.add(velocity));return age>=maxAge;
    }
    private void lookAt(Vec3 eyes){
        double dx=eyes.x-head.getX(),dy=eyes.y-(head.getY()+head.getEyeHeight()),dz=eyes.z-head.getZ();
        if(Math.abs(dx)>1E-5||Math.abs(dz)>1E-5){float target=(float)(Mth.atan2(dz,dx)*180/Math.PI)-90;head.yHeadRot+=Mth.clamp(Mth.wrapDegrees(target-head.yHeadRot),-10,10);}
        head.setXRot(Mth.clamp((float)(-Mth.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI),-40,40));
    }
    void render(Minecraft client,float partial,float strength,PoseStack poses){
        float alpha=HallucinationTiming.lifeAlpha(age,maxAge)*strength;if(alpha<=.001F)return;
        Vec3 position=previousPosition.lerp(head.position(),partial),relative=position.subtract(client.gameRenderer.getMainCamera().getPosition());
        float yaw=Mth.lerp(partial,head.yHeadRotO,head.yHeadRot),pitch=Mth.lerp(partial*partial,head.xRotO,head.getXRot());
        var lightPosition=BlockPos.containing(position.add(0,head.getEyeHeight(),0));int light=client.level.hasChunkAt(lightPosition)?LevelRenderer.getLightColor(client.level,lightPosition):0;
        try(var state=new PostRenderState(false)){
            poses.pushPose();try{
                poses.translate(relative.x,relative.y,relative.z);poses.scale(scale,scale,scale);poses.mulPose(Axis.ZN.rotationDegrees(180));poses.mulPose(Axis.XN.rotationDegrees(pitch));poses.mulPose(Axis.YN.rotationDegrees(180-yaw));
                var buffers=MultiBufferSource.immediate(new BufferBuilder(256));var colored=ApparitionRender.colored(buffers,1,1,1,alpha);
                MODEL.render(poses,colored.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,OverlayTexture.NO_OVERLAY);buffers.endBatch();
            }finally{poses.popPose();}
        }
    }
}
