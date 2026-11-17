package ivorius.psychedelicraft.mixin.client;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Entity.class)
public interface EntityVisualAccessor {
    @Accessor("wasTouchingWater") void psychedelicraft$setTouchingWater(boolean touching);
}
