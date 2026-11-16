/* Furnace hook adapted from Sollace's 1.20.1 MixinAbstractFurnaceBlockEntity. */
package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.entity.drug.SmokeEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Inject(method="serverTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void psychedelicraft$fuelConsumed(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo callback){
        SmokeEffects.fuelConsumed(furnace);
    }
}
