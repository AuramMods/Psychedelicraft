package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.targets.RenderTargets", remap = false)
abstract class OculusHandDepthMixin {
    @Inject(method = "copyPreTranslucentDepth", at = @At("RETURN"), require = 0)
    private void psychedelicraft$captureHandMasks(CallbackInfo ci) {
        ClientEffects.captureOculusHandMasks(this);
    }
}
