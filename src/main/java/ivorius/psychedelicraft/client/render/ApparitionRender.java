/* Copyright (c) 2014, Lukas Tenbrink. Modern models originally adapted by Sollace. */
package ivorius.psychedelicraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.client.render.shader.PostRenderState;
import ivorius.psychedelicraft.mixin.client.EntityDispatcherAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.*;

/** Carry tint to the actual native deferred draw, after its texture alpha test and lighting. */
public final class ApparitionRender {
    private static final float[] WHITE={1,1,1,1};
    private static float[] tint=WHITE;
    private static boolean rendering;
    private ApparitionRender(){}
    public static float[] tint(){return tint;}
    public static boolean isRendering(){return rendering;}
    public static void applyBlend(){
        if(tint==WHITE)return;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        // Iris can set blend factors directly for layers such as glowing eyes.
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glDepthMask(false);
    }
    static void drawEntity(Minecraft client,Entity entity,Vec3[] offsets,Vec3 center,float yaw,float partial,float scale,float red,float green,float blue,float alpha,PoseStack poses){
        var dispatcher=client.getEntityRenderDispatcher();
        boolean shadow=((EntityDispatcherAccessor)dispatcher).psychedelicraft$getShadow(),hitboxes=dispatcher.shouldRenderHitBoxes(),previous=rendering;
        // Oculus's world material programs use the first sixteen texture units.
        try(var state=new PostRenderState(false,16)){
            rendering=true;dispatcher.setRenderShadow(false);dispatcher.setRenderHitBoxes(false);
            var buffers=MultiBufferSource.immediate(new BufferBuilder(256));
            MultiBufferSource colored=colored(buffers,red,green,blue,alpha);
            poses.pushPose();
            try{
                poses.translate(center.x,center.y,center.z);poses.scale(scale,scale,scale);
                int light=dispatcher.getPackedLightCoords(entity,partial);
                for(Vec3 offset:offsets)dispatcher.render(entity,offset.x,offset.y,offset.z,yaw,partial,poses,colored,light);
                buffers.endBatch();
            }finally{poses.popPose();}
        }finally{rendering=previous;dispatcher.setRenderShadow(shadow);dispatcher.setRenderHitBoxes(hitboxes);}
    }
    static MultiBufferSource colored(MultiBufferSource.BufferSource buffers,float red,float green,float blue,float alpha){
        Map<RenderType,RenderType> types=new IdentityHashMap<>();
        float[] color={red,green,blue,alpha};
        return type->buffers.getBuffer(types.computeIfAbsent(type,key->new ColoredType(key,color)));
    }
    private static final class ColoredType extends RenderType {
        private final RenderType delegate;private final float[] color;
        private ColoredType(RenderType delegate,float[] color){
            super("psychedelicraft_apparition",delegate.format(),delegate.mode(),delegate.bufferSize(),delegate.affectsCrumbling(),false,delegate::setupRenderState,delegate::clearRenderState);
            this.delegate=delegate;this.color=color;
        }
        @Override public void end(BufferBuilder builder,VertexSorting sorting){
            float[] previous=tint;tint=color;
            try(var phase=RenderPhase.model()){delegate.end(builder,sorting);}finally{tint=previous;}
        }
    }
}
