package ivorius.psychedelicraft.mixin.client;

import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import ivorius.psychedelicraft.client.render.shader.GeometryProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"com.seibel.distanthorizons.core.render.renderer.DhTerrainShaderProgram",
        "net.irisshaders.iris.compat.dh.IrisLodRenderProgram"}, remap = false)
abstract class DistantTerrainMixin {
    @Unique
    private GeometryProgram psychedelicraft$distantTerrain;

    @Inject(method = "fillUniformData", at = @At("RETURN"), require = 0)
    private void psychedelicraft$uploadDistantTerrain(CallbackInfo ci) {
        if (psychedelicraft$distantTerrain == null) {
            psychedelicraft$distantTerrain = new GeometryProgram(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        }
        GeometryEffects.applyTerrain(psychedelicraft$distantTerrain);
    }
}
