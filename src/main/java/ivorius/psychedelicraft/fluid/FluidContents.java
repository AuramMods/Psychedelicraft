package ivorius.psychedelicraft.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class FluidContents {
    private FluidContents(){}
    public static FluidStack read(CompoundTag data,int capacity){
        Fluid fluid=PSFluids.resolve(data.getString("id"));int amount=Math.min(data.getInt("level"),capacity);
        if(fluid==null||amount<=0)return FluidStack.EMPTY;
        CompoundTag attributes=data.getCompound("attributes");
        return normalize(new FluidStack(fluid,amount,attributes.isEmpty()?null:attributes.copy()));
    }
    public static CompoundTag write(FluidStack fluid){
        CompoundTag result=new CompoundTag();
        if(!fluid.isEmpty()){
            result.putString("id",PSFluids.id(fluid.getFluid()));result.putInt("level",fluid.getAmount());
            if(fluid.hasTag()&&!fluid.getTag().isEmpty())result.put("attributes",fluid.getTag().copy());
        }
        return result;
    }
    public static FluidStack normalize(FluidStack stack){
        if(stack.isEmpty())return FluidStack.EMPTY;
        CompoundTag tag=stack.getTag();return new FluidStack(PSFluids.canonical(stack.getFluid()),stack.getAmount(),tag==null||tag.isEmpty()?null:tag.copy());
    }
}
