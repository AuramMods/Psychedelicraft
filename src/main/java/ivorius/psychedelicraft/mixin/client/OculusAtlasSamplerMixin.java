package ivorius.psychedelicraft.mixin.client;

import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.IntSupplier;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.program.ProgramSamplers$Builder", remap = false)
abstract class OculusAtlasSamplerMixin {
    @Shadow public abstract boolean hasSampler(String name);

    @Inject(method = "build", at = @At("HEAD"), require = 0)
    private void psychedelicraft$registerAtlas(CallbackInfoReturnable<?> cir) {
        if (!hasSampler(GeometryEffects.IRIS_BLOCK_ATLAS)) return;
        try {
            var register = getClass().getMethod("addDynamicSampler", IntSupplier.class, String[].class);
            register.invoke(this, (IntSupplier)GeometryEffects::blockAtlasId, new String[]{GeometryEffects.IRIS_BLOCK_ATLAS});
        } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().debug("Could not register the Psychedelicraft block atlas with Oculus", error);
        }
    }
}
