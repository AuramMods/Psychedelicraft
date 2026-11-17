package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.ClientEffects;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseInertiaMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Unique private double psychedelicraft$rawX,psychedelicraft$rawY;

    @Inject(method="turnPlayer",at=@At("HEAD"))
    private void psychedelicraft$sample(CallbackInfo ci){psychedelicraft$rawX=accumulatedDX;psychedelicraft$rawY=accumulatedDY;}

    @Redirect(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void psychedelicraft$turn(LocalPlayer player,double x,double y){ClientEffects.camera().turn(player,x,y,psychedelicraft$rawX,psychedelicraft$rawY);}
}
