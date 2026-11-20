package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.client.ClientEffects;
import ivorius.psychedelicraft.client.render.CameraEffects;
import ivorius.psychedelicraft.client.render.RenderPhase;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class HandGeometryMixin {
    @Unique private RenderPhase.Scope psychedelicraft$handScope;

    // Oculus also calls this method directly for its shader hand passes.
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void psychedelicraft$beginHands(float partial, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                            LocalPlayer player, int light, CallbackInfo ci) {
        if (ClientEffects.camera().outOfBody(player)) {
            ci.cancel();
            return;
        }
        pose.pushPose();
        psychedelicraft$handScope = RenderPhase.HAND.enter();
        CameraEffects.hand(pose, player, partial);
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void psychedelicraft$endHands(float partial, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                          LocalPlayer player, int light, CallbackInfo ci) {
        if (psychedelicraft$handScope != null) {
            psychedelicraft$handScope.close();
            psychedelicraft$handScope = null;
            pose.popPose();
        }
    }
}
