package ivorius.psychedelicraft.client.render.shader;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;

final class MotionHistory {
    private final RenderTarget[] samples=new RenderTarget[MotionTimeline.SAMPLES];
    private final MotionTimeline timeline=new MotionTimeline();
    private int width,height,sourceWidth,sourceHeight;
    boolean empty(){return timeline.empty();}
    void render(Minecraft client,float ticks,float strength){
        var main=client.getMainRenderTarget();int w=client.getWindow().getGuiScaledWidth(),h=client.getWindow().getGuiScaledHeight();
        if(w!=width||h!=height||main.width!=sourceWidth||main.height!=sourceHeight){release();width=w;height=h;sourceWidth=main.width;sourceHeight=main.height;}
        timeline.update(ticks,index->{if(samples[index]==null)samples[index]=new TextureTarget(width,height,false,Minecraft.ON_OSX);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,samples[index].frameBufferId);GlStateManager._glBlitFrameBuffer(0,0,main.width,main.height,0,0,width,height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);});
        main.bindWrite(true);RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,main.viewWidth,main.viewHeight,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);RenderSystem.getModelViewStack().setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ZERO,GL11.GL_ONE);RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
        for(int n=0;n<samples.length;n++){int index=timeline.index(n);if(samples[index]==null||!timeline.visible(index,ticks))continue;RenderSystem.setShaderColor(1,1,1,timeline.opacity(index,strength));RenderSystem.setShaderTexture(0,samples[index].getColorTextureId());var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);buffer.vertex(0,main.viewHeight,0).uv(0,0).color(255,255,255,255).endVertex();buffer.vertex(main.viewWidth,main.viewHeight,0).uv(1,0).color(255,255,255,255).endVertex();buffer.vertex(main.viewWidth,0,0).uv(1,1).color(255,255,255,255).endVertex();buffer.vertex(0,0,0).uv(0,1).color(255,255,255,255).endVertex();BufferUploader.drawWithShader(buffer.end());}
        RenderSystem.disableBlend();
    }
    void release(){for(int i=0;i<samples.length;i++)if(samples[i]!=null){samples[i].destroyBuffers();samples[i]=null;}timeline.reset();width=height=sourceWidth=sourceHeight=0;}
}
