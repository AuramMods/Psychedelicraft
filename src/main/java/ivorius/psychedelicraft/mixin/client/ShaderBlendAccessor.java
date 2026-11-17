package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.shaders.BlendMode;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderInstance.class)
public interface ShaderBlendAccessor {
    @Accessor("blend") BlendMode psychedelicraft$getBlend();
}
