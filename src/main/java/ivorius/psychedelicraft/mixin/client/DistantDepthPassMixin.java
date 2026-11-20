package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
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
    @Unique private boolean psychedelicraft$observedProjection;

    @Inject(method = PASS, at = @At("HEAD"), require = 0)
    private void psychedelicraft$beginDepthPass(CallbackInfo ci) {
        psychedelicraft$observedProjection = false;
    }

    @Inject(method = PASS, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL32;glDrawElements(IIIJ)V", shift = At.Shift.AFTER), require = 0)
    private void psychedelicraft$observeDepthProjection(@Coerce Object shader, @Coerce Object buffers, @Coerce Object params,
                                                        boolean solid, CallbackInfo ci) {
        if (!psychedelicraft$observedProjection) {
            psychedelicraft$observedProjection = true;
            ClientEffects.observeDistantDepth(params, solid);
        }
    }
}
