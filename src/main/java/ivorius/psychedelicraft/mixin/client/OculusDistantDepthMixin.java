package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.dh.DHCompatInternal", remap = false)
abstract class OculusDistantDepthMixin {
    @Shadow private int storedDepthTex;

    @Inject(method = "copyTranslucents", at = @At("HEAD"), require = 0)
    private void psychedelicraft$preserveOpaqueDepth(int width, int height, CallbackInfo ci) {
        ClientEffects.captureDistantOpaqueDepth(storedDepthTex, width, height);
    }
}
