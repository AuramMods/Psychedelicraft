package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.glObject.shader.Shader", remap = false)
abstract class DistantTerrainSourceMixin {
    @Inject(method = "loadFile", at = @At("RETURN"), cancellable = true, require = 0)
    private static void psychedelicraft$deformDistantTerrain(String path, boolean include, StringBuilder builder,
                                                              CallbackInfoReturnable<StringBuilder> cir) throws IOException {
        if (path.equals("shaders/standard.vert")) {
            cir.setReturnValue(new StringBuilder(GeometryEffects.injectDistantTerrain(cir.getReturnValue().toString())));
        }
    }
}
