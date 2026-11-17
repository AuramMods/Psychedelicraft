package ivorius.psychedelicraft.mixin;
import ivorius.psychedelicraft.fluid.WorldFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Keep native slope search from treating foreign liquids as destinations for a brew. */
@Mixin(FlowingFluid.class)
public abstract class WorldFluidFlowMixin {
    @Inject(method="canHoldFluid",at=@At("HEAD"),cancellable=true)
    private void psychedelicraft$keepForeignLiquids(BlockGetter level,BlockPos pos,BlockState state,Fluid incoming,CallbackInfoReturnable<Boolean> result){
        if((Object)this instanceof WorldFluid world&&!state.getFluidState().isEmpty()&&!world.isSame(state.getFluidState().getType()))result.setReturnValue(false);
    }
}
