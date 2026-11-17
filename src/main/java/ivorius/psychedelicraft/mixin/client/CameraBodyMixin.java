package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
abstract class CameraBodyMixin {
    @Inject(method="isDetached",at=@At("RETURN"),cancellable=true)
    private void psychedelicraft$body(CallbackInfoReturnable<Boolean> ci){
        if(!ci.getReturnValue()&&ClientEffects.camera().outOfBody(((Camera)(Object)this).getEntity()))ci.setReturnValue(true);
    }
}
