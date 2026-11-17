package ivorius.psychedelicraft.mixin.client;
import ivorius.psychedelicraft.client.render.ContextualHallucinations;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EntityRenderDispatcher.class)
abstract class IdentityRenderMixin {
    @ModifyVariable(method="render",at=@At("HEAD"),argsOnly=true)
    private Entity psychedelicraft$disguise(Entity original){return ContextualHallucinations.forRender(original);}
}
