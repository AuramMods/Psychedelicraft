/* Preserve the accepted water-like brew interactions through native conduits. */
package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitFluidMixin {
    @Redirect(method="applyEffects",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private static boolean psychedelicraft$submergedPlayer(Player player){
        return player.isInWaterOrRain()||player.isInFluidType((type,height)->PSFluids.isWaterLike(type));
    }
    // Include the native synthetic targeting predicate without depending on its generated name.
    @Redirect(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;isInWaterOrRain()Z"))
    private static boolean psychedelicraft$submergedTarget(LivingEntity entity){
        return entity.isInWaterOrRain()||entity.isInFluidType((type,height)->PSFluids.isWaterLike(type));
    }
}
