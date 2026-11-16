/* Native Forge fluid contact for the donor's contained mash-tub fluids. */
package ivorius.psychedelicraft.mixin;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Predicate;
@Mixin(Entity.class)
public abstract class EntityMashTubFluidMixin {
    @Shadow(remap=false) protected Object2DoubleMap<FluidType> forgeFluidTypeHeight;
    @Shadow(remap=false) private FluidType forgeFluidTypeOnEyes;
    @Inject(method="updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V",at=@At("RETURN"),remap=false)
    private void psychedelicraft$basinBody(Predicate<FluidState> filter,CallbackInfo ci){
        Entity entity=(Entity)(Object)this;AABB bounds=entity.getBoundingBox().deflate(.001);var world=entity.level();
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ))){
            var vat=MashTubBlock.getVat(world,pos);if(vat==null)continue;var fluid=vat.getTank().getFluid();if(fluid.isEmpty()||fluid.getAmount()*8L<vat.getTank().getCapacity())continue;
            FluidType type=fluid.getFluid()==Fluids.LAVA?ForgeMod.LAVA_TYPE.get():fluid.getFluid()==Fluids.WATER||PSFluids.id(fluid.getFluid()).startsWith("psychedelicraft:")?ForgeMod.WATER_TYPE.get():null;
            var owner=vat.getBlockPos();if(type==null||!bounds.intersects(new AABB(owner.getX()-.4375,owner.getY()+.0625,owner.getZ()-.4375,owner.getX()+1.4375,owner.getY()+vat.getFluidSurface(),owner.getZ()+1.4375))||!filter.test((type==ForgeMod.LAVA_TYPE.get()?Fluids.LAVA:Fluids.WATER).defaultFluidState()))continue;
            double height=fluid.getAmount()*8/vat.getTank().getCapacity();forgeFluidTypeHeight.put(type,Math.max(forgeFluidTypeHeight.getDouble(type),height));
        }
    }
    @Inject(method="updateFluidOnEyes",at=@At("RETURN"))
    private void psychedelicraft$basinEyes(CallbackInfo ci){
        Entity entity=(Entity)(Object)this;var eye=entity.getEyePosition().add(0,-.1111111119389534,0);var vat=MashTubBlock.getVat(entity.level(),BlockPos.containing(eye));if(vat==null||vat.getTank().isEmpty())return;
        if(entity.getVehicle() instanceof Boat boat&&!boat.isUnderWater()&&boat.getBoundingBox().minY<=eye.y&&boat.getBoundingBox().maxY>=eye.y)return;
        var owner=vat.getBlockPos();if(eye.x<=owner.getX()-.4375||eye.x>=owner.getX()+1.4375||eye.z<=owner.getZ()-.4375||eye.z>=owner.getZ()+1.4375||eye.y<owner.getY()+.0625||eye.y>=owner.getY()+vat.getFluidSurface())return;
        var fluid=vat.getTank().getFluid().getFluid();if(fluid==Fluids.LAVA)forgeFluidTypeOnEyes=ForgeMod.LAVA_TYPE.get();else if(fluid==Fluids.WATER||PSFluids.id(fluid).startsWith("psychedelicraft:"))forgeFluidTypeOnEyes=ForgeMod.WATER_TYPE.get();
    }
}
