/* Copyright (c) 2014, Lukas Tenbrink. Native alt-font drawing adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.client.render.blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.*;
final class RiftJarGlyphs {
    private static final RiftGlyphPath OUTGOING=RiftGlyphPath.spiral(new Vec3(0,6,0),.06,6,6),SPHERE=RiftGlyphPath.sphere(3,8);
    private final Map<RiftJarBlockEntity.Connection,RiftGlyphPath> paths=new WeakHashMap<>();
    void render(RiftJarBlockEntity jar,PoseStack pose,MultiBufferSource buffers,int light,float ticks,float outgoing){
        MultiBufferSource twoSided=type->buffers.getBuffer(GlyphLayer.of(type));
        float shift=ticks*-.002F;
        for(var connection:jar.getConnections()){
            var target=connection.getPosition().subtract(jar.getBlockPos().getX()+.5,jar.getBlockPos().getY()+.6,jar.getBlockPos().getZ()+.5);
            var path=paths.computeIfAbsent(connection,c->RiftGlyphPath.spiral(target,.1,.5,8));draw("This is a small spiral.",path,pose,twoSided,light,shift,connection.getStrength(),0);
            pose.pushPose();pose.translate(target.x,target.y,target.z);draw("This is a small circle.",SPHERE,pose,twoSided,light,shift,1,1-connection.getStrength());pose.popPose();
        }
        if(outgoing>0)draw("This is a small spiral.",OUTGOING,pose,twoSided,light,shift,outgoing,0);
    }
    private void draw(String label,RiftGlyphPath path,PoseStack pose,MultiBufferSource buffers,int light,float shift,float cap,float hidden){
        var mask=new Random(42);
        for(int i=0;i<label.length();i++){
            char c=label.charAt(i);boolean masked=hidden>0&&mask.nextFloat()<=hidden;double progress=((i/(float)label.length()+shift)%1D+1)%1D;if(masked||c==' '||progress>cap)continue;
            var point=path.at(progress);var tangent=path.at((float)progress+.003).subtract(point);double length=tangent.length();float yaw=(float)(Math.atan2(tangent.y,tangent.z)/(Math.PI*180))+180,pitch=length==0?90:(float)(Math.acos(Mth.clamp(tangent.x/length,-1,1))/(Math.PI*180)+90);
            pose.pushPose();pose.translate(point.x,point.y,point.z);pose.scale(-1/60F,-1/60F,-1/60F);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(pitch));
            var text=Component.literal(String.valueOf(c)).withStyle(style->style.withFont(new ResourceLocation("alt")));
            Minecraft.getInstance().font.drawInBatch(text,0,0,0xffffffff,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,light);pose.popPose();
        }
    }
    /** Keep the donor's two-sided glyphs while retaining the native font texture, shader and lightmap. */
    private static final class GlyphLayer extends RenderType {
        private static final Map<RenderType,RenderType> CACHE=new IdentityHashMap<>();
        static RenderType of(RenderType original){return CACHE.computeIfAbsent(original,GlyphLayer::new);}
        private GlyphLayer(RenderType original){
            super("psychedelicraft_rift_glyph",original.format(),original.mode(),original.bufferSize(),false,true,
                    ()->{original.setupRenderState();RenderSystem.disableCull();},
                    ()->{original.clearRenderState();RenderSystem.enableCull();});
        }
    }
}
