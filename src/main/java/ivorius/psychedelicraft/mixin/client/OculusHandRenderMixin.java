package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
import ivorius.psychedelicraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
abstract class OculusHandRenderMixin {
    @Unique private RenderPhase.Scope psychedelicraft$hand;

    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("HEAD"), require = 0)
    private void psychedelicraft$beginHand(CallbackInfo ci) {
        psychedelicraft$hand = RenderPhase.HAND.enter();
    }

    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("RETURN"), require = 0)
    private void psychedelicraft$endHand(CallbackInfo ci) {
        psychedelicraft$hand.close();
    }
    @Inject(method = "renderTranslucent", at = @At("HEAD"), require = 0)
    private void psychedelicraft$captureWorldWithoutTranslucentHand(CallbackInfo ci) {
        ClientEffects.captureBeforeOculusTranslucentHand();
    }
}
