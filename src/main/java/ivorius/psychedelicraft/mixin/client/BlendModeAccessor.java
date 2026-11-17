package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.shaders.BlendMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlendMode.class)
public interface BlendModeAccessor {
    @Accessor("lastApplied") static BlendMode psychedelicraft$getLastApplied(){throw new AssertionError();}
    @Accessor("lastApplied") static void psychedelicraft$setLastApplied(BlendMode value){throw new AssertionError();}
}
