package ivorius.psychedelicraft.mixin.client;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(WalkAnimationState.class)
public interface WalkAnimationAccessor {
    @Accessor("speedOld") float psychedelicraft$getSpeedOld();
    @Accessor("speedOld") void psychedelicraft$setSpeedOld(float speed);
    @Accessor("speed") float psychedelicraft$getSpeed();
    @Accessor("speed") void psychedelicraft$setSpeed(float speed);
    @Accessor("position") float psychedelicraft$getPosition();
    @Accessor("position") void psychedelicraft$setPosition(float position);
}
