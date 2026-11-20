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
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface", remap = false)
abstract class EmbeddiumTerrainMixin {
    @Unique
    private GeometryProgram psychedelicraft$geometry;

    @Inject(method = "setupState", at = @At("RETURN"), require = 0)
    private void psychedelicraft$uploadTerrain(CallbackInfo ci) {
        if (psychedelicraft$geometry == null) {
            psychedelicraft$geometry = new GeometryProgram(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        }
        GeometryEffects.applyTerrain(psychedelicraft$geometry);
    }
}
