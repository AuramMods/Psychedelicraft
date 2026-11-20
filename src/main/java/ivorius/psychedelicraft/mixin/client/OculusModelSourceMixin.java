package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Map;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
abstract class OculusModelSourceMixin {
    @Inject(method = "patchVanilla", at = @At("RETURN"), cancellable = true, require = 0)
    private static void psychedelicraft$composeModel(String name, String vertex, String geometry, String tessControl,
                                                      String tessEvaluation, String fragment, @Coerce Object alphaTest,
                                                      boolean lines, boolean intensity, @Coerce Object inputs, @Coerce Object textures,
                                                      CallbackInfoReturnable<Map<?, String>> cir) throws IOException {
        if (!name.startsWith("entities_") && !name.startsWith("hand_") && !name.startsWith("block_entity")
                && !name.equals("moving_block")) return;
        var stages = cir.getReturnValue();
        var patched = GeometryEffects.injectIrisModel(stages);
        if (stages != patched) cir.setReturnValue(patched);
    }
}
