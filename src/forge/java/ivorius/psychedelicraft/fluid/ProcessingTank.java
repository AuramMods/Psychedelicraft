package ivorius.psychedelicraft.fluid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import javax.annotation.Nullable;

public final class ProcessingTank extends FluidTank {
    private final Runnable changed;

    public ProcessingTank(int capacity, Runnable changed) {
        super(capacity);
        this.changed = changed;
    }

    @Override
    public int fill(@Nullable FluidStack resource, boolean doFill) {
        return super.fill(FluidContents.normalize(resource), doFill);
    }

    public boolean fillExactly(FluidStack resource, boolean doFill) {
        if (resource.amount <= 0 || fill(resource, false) != resource.amount) return false;
        return !doFill || fill(resource, true) == resource.amount;
    }

    @Override
    public FluidStack drain(@Nullable FluidStack resource, boolean doDrain) {
        return super.drain(FluidContents.normalize(resource), doDrain);
    }

    public void replace(@Nullable FluidStack contents) {
        setFluid(contents == null || contents.amount <= 0 ? null
                : new FluidStack(FluidContents.normalize(contents), Math.min(contents.amount, capacity)));
        onContentsChanged();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.merge(FluidContents.write(fluid));
        return nbt;
    }

    @Override
    public FluidTank readFromNBT(NBTTagCompound nbt) {
        setFluid(FluidContents.read(nbt, capacity));
        return this;
    }

    @Override
    protected void onContentsChanged() {
        changed.run();
    }
}
