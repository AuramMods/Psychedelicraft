package ivorius.psychedelicraft.client.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.mixin.client.BlendModeAccessor;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

/** Restore the native render caches together with the actual GL bindings. */
final class PostRenderState implements AutoCloseable {
    private final int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),arrayBuffer=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    private final int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int equationRgb=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),equationAlpha=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),stencil=GL11.glIsEnabled(GL11.GL_STENCIL_TEST),depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
    private final int[] viewport=new int[4],scissorBox=new int[4],textures=new int[3],shaderTextures=new int[3];
    private final float[] clearColor=new float[4],shaderColor=RenderSystem.getShaderColor().clone();
    private final ByteBuffer colorMask=BufferUtils.createByteBuffer(4);
    private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());
    private final VertexSorting sorting=RenderSystem.getVertexSorting();
    private final ShaderInstance shader=RenderSystem.getShader();
    private final BlendMode lastBlend=BlendModeAccessor.psychedelicraft$getLastApplied();
    PostRenderState(){
        GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissorBox);GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clearColor);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,colorMask);
        for(int i=0;i<textures.length;i++){GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);textures[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);shaderTextures[i]=RenderSystem.getShaderTexture(i);}GlStateManager._activeTexture(activeTexture);
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.disableScissor();GL11.glDisable(GL11.GL_STENCIL_TEST);RenderSystem.disableCull();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.colorMask(true,true,true,true);RenderSystem.disableBlend();
    }
    @Override public void close(){
        RenderSystem.getModelViewStack().popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setShaderColor(shaderColor[0],shaderColor[1],shaderColor[2],shaderColor[3]);RenderSystem.setShader(()->shader);
        for(int i=0;i<textures.length;i++){RenderSystem.setShaderTexture(i,shaderTextures[i]);GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);GlStateManager._bindTexture(textures[i]);}GlStateManager._activeTexture(activeTexture);
        GlStateManager._glUseProgram(program);RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);GL20.glBlendEquationSeparate(equationRgb,equationAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();BlendModeAccessor.psychedelicraft$setLastApplied(lastBlend);
        RenderSystem.depthFunc(depthFunc);RenderSystem.depthMask(depthMask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();if(stencil)GL11.glEnable(GL11.GL_STENCIL_TEST);else GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.colorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);RenderSystem.clearColor(clearColor[0],clearColor[1],clearColor[2],clearColor[3]);GlStateManager._clearDepth(clearDepth);
        if(scissor)RenderSystem.enableScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);else RenderSystem.disableScissor();
        BufferUploader.reset();GlStateManager._glBindVertexArray(vao);GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER,arrayBuffer);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
    }
}
