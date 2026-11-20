package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
abstract class EmbeddiumShaderSourceMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, require = 0)
    private static void psychedelicraft$extendTerrain(ResourceLocation id, CallbackInfoReturnable<String> cir) throws IOException {
        if (!id.getNamespace().equals("sodium")) return;
        switch (id.getPath()) {
            case "blocks/block_layer_opaque.vsh" -> cir.setReturnValue(GeometryEffects.injectTerrain(cir.getReturnValue(), true));
            case "blocks/block_layer_opaque.fsh" -> cir.setReturnValue(GeometryEffects.injectTerrain(cir.getReturnValue(), false));
        }
    }
}
