package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.ModelRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(PistonHeadRenderer.class)
abstract class PistonGeometryMixin {
    @ModifyVariable(method="render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("HEAD"),argsOnly=true)
    private MultiBufferSource psychedelicraft$movingModelBuffers(MultiBufferSource buffers){return type->buffers.getBuffer(ModelRenderType.of(type));}
}
