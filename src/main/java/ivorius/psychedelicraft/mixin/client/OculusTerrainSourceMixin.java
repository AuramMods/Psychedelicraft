package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.util.Map;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
abstract class OculusTerrainSourceMixin {
    @Inject(method = "patchSodium", at = @At("RETURN"), cancellable = true, require = 0)
    private static void psychedelicraft$nearTerrain(String name, String vertex, String geometry, String tessControl,
                                                     String tessEvaluation, String fragment, @Coerce Object alphaTest,
                                                     @Coerce Object inputs, @Coerce Object textures,
                                                     CallbackInfoReturnable<Map<?, String>> cir) throws IOException {
        psychedelicraft$composeTerrain(cir, !name.startsWith("shadow"));
    }

    @Inject(method = "patchDHTerrain", at = @At("RETURN"), cancellable = true, require = 0)
    private static void psychedelicraft$distantTerrain(CallbackInfoReturnable<Map<?, String>> cir) throws IOException {
        psychedelicraft$composeTerrain(cir, false);
    }

    @Unique
    private static void psychedelicraft$composeTerrain(CallbackInfoReturnable<Map<?, String>> cir, boolean material) throws IOException {
        var stages = cir.getReturnValue();
        var patched = GeometryEffects.injectIrisTerrain(stages, material);
        if (stages != patched) cir.setReturnValue(patched);
    }
}
