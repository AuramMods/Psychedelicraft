/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/** Native Forge storage with the accepted save format and independent read snapshots. */
public final class ProcessingTank extends FluidTank {
    private final Runnable changed;
    private boolean canFill=true,canDrain=true;
    public void setCanFill(boolean value){canFill=value;}
    public void setCanDrain(boolean value){canDrain=value;}
    public ProcessingTank(int capacity,Runnable changed){super(capacity);this.changed=changed;}
    @Override public FluidStack getFluid(){return fluid.copy();}
    @Override public int fill(FluidStack resource,FluidAction action){return canFill?super.fill(FluidContents.normalize(resource),action):0;}
    @Override public FluidStack drain(FluidStack resource,FluidAction action){return canDrain?super.drain(FluidContents.normalize(resource),action):FluidStack.EMPTY;}
    @Override public FluidStack drain(int amount,FluidAction action){return canDrain?super.drain(amount,action):FluidStack.EMPTY;}
    public boolean fillExactly(FluidStack resource,FluidAction action){
        if(resource.isEmpty()||fill(resource,FluidAction.SIMULATE)!=resource.getAmount())return false;
        return action.simulate()||fill(resource,FluidAction.EXECUTE)==resource.getAmount();
    }
    public void replace(FluidStack supplied){setFluid(supplied);onContentsChanged();}
    @Override public void setFluid(FluidStack supplied){
        FluidStack normalized=FluidContents.normalize(supplied);
        if(!normalized.isEmpty())normalized.setAmount(Math.min(capacity,normalized.getAmount()));
        super.setFluid(normalized);
    }
    @Override public CompoundTag writeToNBT(CompoundTag tag){tag.merge(FluidContents.write(fluid));return tag;}
    @Override public ProcessingTank readFromNBT(CompoundTag tag){setFluid(FluidContents.read(tag,capacity));return this;}
    @Override protected void onContentsChanged(){changed.run();}
}
