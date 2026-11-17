package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.shaders.Uniform;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ShaderInstance.class)
abstract class GeometryShaderMixin {
    @Shadow @Final private List<Uniform> uniforms;
    @Shadow @Final private List<String> samplerNames;
    @Inject(method="updateLocations",at=@At("HEAD"))
    private void psychedelicraft$geometryLocations(CallbackInfo ci){GeometryEffects.register((ShaderInstance)(Object)this,uniforms,samplerNames);}
    @Inject(method="apply",at=@At("HEAD"))
    private void psychedelicraft$geometryValues(CallbackInfo ci){GeometryEffects.apply((ShaderInstance)(Object)this);}
    @Inject(method="apply",at=@At("RETURN"))
    private void psychedelicraft$apparitionBlend(CallbackInfo ci){ivorius.psychedelicraft.client.render.ApparitionRender.applyBlend();}
}
