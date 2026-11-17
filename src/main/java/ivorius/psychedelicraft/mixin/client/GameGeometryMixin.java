package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.client.render.RenderPhase;
import ivorius.psychedelicraft.client.render.CameraEffects;
import ivorius.psychedelicraft.client.ClientEffects;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameGeometryMixin {
    @Unique private RenderPhase.Scope psychedelicraft$worldScope,psychedelicraft$screenScope;
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void psychedelicraft$beginWorld(float partial,long finish,PoseStack pose,CallbackInfo ci){GeometryEffects.beginFrame(partial);psychedelicraft$worldScope=RenderPhase.WORLD.enter();}
    @Inject(method="renderLevel",at=@At("RETURN"))
    private void psychedelicraft$endWorld(float partial,long finish,PoseStack pose,CallbackInfo ci){psychedelicraft$worldScope.close();}
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack$Pose;normal()Lorg/joml/Matrix3f;"))
    private void psychedelicraft$camera(float partial,long finish,PoseStack pose,CallbackInfo ci){ClientEffects.camera().view(pose,((GameRenderer)(Object)this).getMainCamera(),partial);}
    @Inject(method="renderItemInHand",at=@At("HEAD"))
    private void psychedelicraft$beginScreen(PoseStack pose,Camera camera,float partial,CallbackInfo ci){psychedelicraft$screenScope=RenderPhase.SCREEN.enter();}
    @Inject(method="renderItemInHand",at=@At("RETURN"))
    private void psychedelicraft$endScreen(PoseStack pose,Camera camera,float partial,CallbackInfo ci){psychedelicraft$screenScope.close();}
    @Redirect(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    private void psychedelicraft$hands(ItemInHandRenderer renderer,float partial,PoseStack pose,MultiBufferSource.BufferSource buffers,LocalPlayer player,int light){
        if(ClientEffects.camera().outOfBody(player))return;
        pose.pushPose();
        try(var scope=RenderPhase.HAND.enter()){
            CameraEffects.hand(pose,player,partial);
            renderer.renderHandsWithItems(partial,pose,buffers,player,light);
        }finally{pose.popPose();}
    }
}
