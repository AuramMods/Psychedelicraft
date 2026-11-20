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
        var stages = cir.getReturnValue();
        var patched = switch (name) {
            case "shadow_entities_cutout", "shadow_beacon_beam", "shadow_particles", "shadow_clouds", "shadow_lightning" ->
                    GeometryEffects.injectIrisShadowModel(stages);
            case "moving_block", "be_translucent", "particles", "particles_trans", "weather", "glint",
                    "sky_basic", "sky_basic_color", "sky_textured", "sky_textured_color", "clouds", "clouds_sodium" ->
                    GeometryEffects.injectIrisModel(stages);
            default -> name.startsWith("entities_") || name.startsWith("hand_") || name.startsWith("block_entity")
                    ? GeometryEffects.injectIrisModel(stages) : stages;
        };
        if (stages != patched) cir.setReturnValue(patched);
    }
}
