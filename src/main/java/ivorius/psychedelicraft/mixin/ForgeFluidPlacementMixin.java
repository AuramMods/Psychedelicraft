package ivorius.psychedelicraft.mixin;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Forge's generic state-only wrapper cannot carry arbitrary batch NBT. */
@Mixin(value=FluidUtil.class,remap=false)
public abstract class ForgeFluidPlacementMixin {
    @Inject(method="tryPickUpFluid",at=@At("HEAD"),cancellable=true)
    private static void psychedelicraft$namedWorldPickup(net.minecraft.world.item.ItemStack empty,Player player,Level level,BlockPos pos,net.minecraft.core.Direction side,CallbackInfoReturnable<FluidActionResult> result){
        if(level==null||pos==null||!level.hasChunkAt(pos)||!(level.getBlockState(pos).getBlock() instanceof ivorius.psychedelicraft.block.PhysicalFluidBlock block))return;
        var vessel=FluidVesselHandler.forCrafting(empty);if(vessel==null)return;
        var available=block.drain(level,pos,IFluidHandler.FluidAction.SIMULATE);
        if(available.isEmpty()||vessel.fill(available,IFluidHandler.FluidAction.SIMULATE)!=1000){result.setReturnValue(FluidActionResult.FAILURE);return;}
        var moved=block.drain(level,pos,IFluidHandler.FluidAction.EXECUTE);
        if(moved.isEmpty()){result.setReturnValue(FluidActionResult.FAILURE);return;}
        vessel.fill(moved,IFluidHandler.FluidAction.EXECUTE);result.setReturnValue(new FluidActionResult(empty.is(net.minecraft.world.item.Items.BUCKET)?ivorius.psychedelicraft.item.FilledBucketItem.filled(moved,empty):vessel.getContainer()));
    }
    @Inject(method="getFluidBlockHandler",at=@At("HEAD"),cancellable=true)
    private static void psychedelicraft$batchPlacement(Fluid fluid,Level level,BlockPos pos,CallbackInfoReturnable<IFluidHandler> result){
        if(fluid instanceof WorldFluid world)result.setReturnValue(new FluidBlockWrapper(world.family.block.get(),level,pos));
    }
    @Inject(method="tryPlaceFluid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/minecraftforge/fluids/capability/IFluidHandler;Lnet/minecraftforge/fluids/FluidStack;)Z",at=@At("HEAD"),cancellable=true)
    private static void psychedelicraft$batchVat(Player player,Level level,InteractionHand hand,BlockPos pos,IFluidHandler source,FluidStack supplied,CallbackInfoReturnable<Boolean> result){
        if(!(supplied.getFluid() instanceof WorldFluid)||level==null||pos==null||!level.hasChunkAt(pos))return;
        var vat=MashTubBlock.getVat(level,pos);if(vat==null)return;
        var portion=supplied.copy();portion.setAmount(1000);
        boolean accepts=supplied.getAmount()>=1000&&source.drain(portion,IFluidHandler.FluidAction.SIMULATE).getAmount()==1000&&vat.getTank().fillExactly(portion,IFluidHandler.FluidAction.SIMULATE);
        if(accepts&&!level.isClientSide){accepts=FluidUtil.tryFluidTransfer(vat.getTank(),source,portion,true).getAmount()==1000;if(accepts)level.playSound(player,pos,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
        result.setReturnValue(accepts);
    }
}
