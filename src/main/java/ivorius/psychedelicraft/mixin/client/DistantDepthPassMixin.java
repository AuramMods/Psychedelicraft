package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.renderer.LodRenderer", remap = false)
abstract class DistantDepthPassMixin {
    @Unique private static final String PASS = "renderLodPass(Lcom/seibel/distanthorizons/api/interfaces/override/rendering/IDhApiShaderProgram;"
            + "Lcom/seibel/distanthorizons/core/render/RenderBufferHandler;Lcom/seibel/distanthorizons/core/render/renderer/RenderParams;Z)V";
    @Unique private static final String DRAW = "Lorg/lwjgl/opengl/GL32;glDrawElements(IIIJ)V";
    // The native DH program reserves unit zero for its lightmap.
    @Unique private static final int ATLAS_UNIT = 1;
    @Unique private boolean psychedelicraft$observedProjection;
    @Unique private int psychedelicraft$previousAtlasTexture = -1;
    @Unique private int psychedelicraft$previousAtlasSampler;

    @Inject(method = PASS, at = @At("HEAD"), require = 0)
    private void psychedelicraft$beginDepthPass(CallbackInfo ci) {
        psychedelicraft$observedProjection = false;
    }

    @Inject(method = PASS, at = @At(value = "INVOKE", target = DRAW), require = 0)
    private void psychedelicraft$bindNativeAtlas(CallbackInfo ci) {
        if (psychedelicraft$observedProjection) return;
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        int atlas = GL20.glGetUniformLocation(program, GeometryEffects.DH_BLOCK_ATLAS);
        if (atlas < 0) return;
        int strength = GL20.glGetUniformLocation(program, "PS_SurfaceFractalStrength");
        GL20.glUniform1i(atlas, ATLAS_UNIT);
        if (strength < 0 || GL20.glGetUniformf(program, strength) <= 0) return;
        int texture = GeometryEffects.blockAtlasId();
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + ATLAS_UNIT);
        psychedelicraft$previousAtlasTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        if (GL.getCapabilities().glBindSampler != 0L) {
            psychedelicraft$previousAtlasSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
            GL33.glBindSampler(ATLAS_UNIT, 0);
        }
        GL13.glActiveTexture(active);
    }

    @Inject(method = PASS, at = @At("RETURN"), require = 0)
    private void psychedelicraft$restoreNativeAtlas(CallbackInfo ci) {
        if (psychedelicraft$previousAtlasTexture < 0) return;
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + ATLAS_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, psychedelicraft$previousAtlasTexture);
        if (GL.getCapabilities().glBindSampler != 0L) {
            GL33.glBindSampler(ATLAS_UNIT, psychedelicraft$previousAtlasSampler);
        }
        GL13.glActiveTexture(active);
        psychedelicraft$previousAtlasTexture = -1;
    }

    @Inject(method = PASS, at = @At(value = "INVOKE", target = DRAW, shift = At.Shift.AFTER), require = 0)
    private void psychedelicraft$observeDepthProjection(@Coerce Object shader, @Coerce Object buffers, @Coerce Object params,
                                                        boolean solid, CallbackInfo ci) {
        if (!psychedelicraft$observedProjection) {
            psychedelicraft$observedProjection = true;
            ClientEffects.observeDistantDepth(params, solid);
        }
    }
}
