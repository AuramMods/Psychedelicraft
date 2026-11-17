/* Copyright (c) 2014, Lukas Tenbrink. Native model/renderer design by Sollace. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.RiftJarBlock;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.client.render.ZeroMatter;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

public final class RiftJarRenderer implements BlockEntityRenderer<RiftJarBlockEntity> {
    public static final ResourceLocation TEXTURE=Psychedelicraft.id("textures/entity/rift_jar/rift_jar.png"),CRACKED=Psychedelicraft.id("textures/entity/rift_jar/rift_jar_cracked.png");
    private final RiftJarModel model=new RiftJarModel(RiftJarModel.getLayerDefinition().bakeRoot());
    private final RiftJarGlyphs glyphs=new RiftJarGlyphs();
    public RiftJarRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(RiftJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float ticks=jar.getVisualAge()+partial;pose.pushPose();pose.translate(.5,.5,.5);pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(90-jar.getBlockState().getValue(RiftJarBlock.FACING).toYRot()));model.setAngles(jar,partial);pose.translate(0,1.001,0);pose.mulPose(Axis.XP.rotationDegrees(180));
        model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,overlay,1,1,1,1);
        float cracked=jar.isBroken()?1:Math.min((jar.getAmount()-.5F)*2,1);if(cracked>0)model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(CRACKED)),light,overlay,1,1,1,cracked);
        if(jar.getAmount()>0){pose.pushPose();pose.translate(0,1.5,0);pose.mulPose(Axis.XN.rotationDegrees(180));pose.scale(.9F,1,.9F);model.renderInterior(pose,buffers.getBuffer(ZeroMatter.layer(ticks)),0,0,1,1,1,Math.min(jar.getAmount()*2,1));pose.popPose();}
        pose.popPose();glyphs.render(jar,pose,buffers,light,ticks,jar.getHandle(partial)*jar.getLid(partial));pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(RiftJarBlockEntity jar){return !jar.getConnections().isEmpty()||jar.getHandle(1)*jar.getLid(1)>0;}
}
