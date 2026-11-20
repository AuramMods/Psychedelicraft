package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.ApparitionRender;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Supplies the tint to Oculus's program after its own shader binding and uploads. */
@Pseudo
@Mixin(targets = {"net.irisshaders.iris.pipeline.programs.ExtendedShader",
        "net.irisshaders.iris.pipeline.programs.FallbackShader"}, remap = false)
abstract class OculusShaderTintMixin {
    @Unique
    private int psychedelicraft$tintLocation = -2;

    @Inject(method = {"m_173363_", "apply"}, at = @At("RETURN"), require = 0)
    private void psychedelicraft$uploadTint(CallbackInfo ci) {
        if (psychedelicraft$tintLocation == -2) {
            int program = ((ShaderInstance) (Object) this).getId();
            psychedelicraft$tintLocation = GL20.glGetUniformLocation(program, "PS_ApparitionTint");
        }
        if (psychedelicraft$tintLocation >= 0) {
            float[] tint = ApparitionRender.tint();
            GL20.glUniform4f(psychedelicraft$tintLocation, tint[0], tint[1], tint[2], tint[3]);
        }
    }
}
