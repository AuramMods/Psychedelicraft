package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Map;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
abstract class OculusTerrainSourceMixin {
    @Inject(method = {"patchSodium", "patchDHTerrain"}, at = @At("RETURN"), cancellable = true, require = 0)
    private static void psychedelicraft$deformTerrain(CallbackInfoReturnable<Map<?, String>> cir) throws IOException {
        var stages = cir.getReturnValue();
        var patched = GeometryEffects.injectIrisTerrain(stages);
        if (stages != patched) cir.setReturnValue(patched);
    }
}
