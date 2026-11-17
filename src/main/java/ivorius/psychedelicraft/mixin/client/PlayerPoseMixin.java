package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.client.render.PlayerPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(LivingEntityRenderer.class)
abstract class PlayerPoseMixin {
    @Shadow protected EntityModel model;
    @Unique private final Deque<PlayerPose> psychedelicraft$poses=new ArrayDeque<>();

    // A marker also balances Forge-cancelled renders and recursively reused renderers.
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void psychedelicraft$begin(LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){psychedelicraft$poses.push(PlayerPose.EMPTY);}

    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void psychedelicraft$pose(LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
        psychedelicraft$poses.pop();
        psychedelicraft$poses.push(PlayerPose.apply(model,entity,entity.tickCount+partial));
    }

    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"))
    private void psychedelicraft$restore(LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){psychedelicraft$poses.pop().close();}
}
