package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.ApparitionRender;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import ivorius.psychedelicraft.client.render.shader.GeometryProgram;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supplies the geometry state after Oculus binds and updates its model shader. */
@Pseudo
@Mixin(targets = {"net.irisshaders.iris.pipeline.programs.ExtendedShader",
        "net.irisshaders.iris.pipeline.programs.FallbackShader"}, remap = false)
abstract class OculusShaderGeometryMixin {
    @Unique
    private GeometryProgram psychedelicraft$geometry;

    @Inject(method = {"m_173363_", "apply"}, at = @At("RETURN"), require = 0)
    private void psychedelicraft$uploadGeometry(CallbackInfo ci) {
        var shader = (ShaderInstance)(Object)this;
        if (psychedelicraft$geometry == null) {
            psychedelicraft$geometry = new GeometryProgram(shader.getId());
        }
        GeometryEffects.applyIrisModel(shader.getName(), psychedelicraft$geometry);
        ApparitionRender.applyBlend();
    }
}
