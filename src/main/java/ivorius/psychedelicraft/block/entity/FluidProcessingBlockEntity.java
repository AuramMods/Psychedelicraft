/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.BrewProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class FluidProcessingBlockEntity extends FluidMachineBlockEntity {
    private BrewProcess stage = BrewProcess.IDLE;
    private FluidStack batch;
    private int elapsed;
    private int duration;
    private ItemStack solidOutput = ItemStack.EMPTY;

    protected FluidProcessingBlockEntity(int capacity) {
        super(capacity);
    }

    public final BrewProcess getStage() {
        return stage;
    }

    public final int getRemainingTicks() {
        return Math.max(0, duration - elapsed);
    }

    public float getProcessingProgress() {
        return duration > 0 ? (float) elapsed / duration : 0;
    }

    public final ItemStack getSolidOutput() {
        return solidOutput;
    }

    public final boolean collectSolidOutput(EntityPlayer player) {
        ItemStack output = takeSolidOutput();
        if (output.isEmpty()) return false;
        ItemHandlerHelper.giveItemToPlayer(player, output);
        return true;
    }

    protected final ItemStack takeSolidOutput() {
        ItemStack output = solidOutput;
        if (!output.isEmpty()) {
            solidOutput = ItemStack.EMPTY;
            changed();
        }
        return output;
    }

    @Override
    protected final void onTankContentsChanged() {
        FluidStack contents = getTank().getFluid();
        if (contents == null || !contents.isFluidEqual(batch)) {
            batch = contents == null ? null : contents.copy();
            reset(BrewProcess.IDLE, 0);
        }
    }

    protected final void processFluid(boolean aging) {
        processFluid(BrewProcess.find(getTank().getFluid(), aging, solidOutput.getCount() < 64), 1, true);
    }

    protected final void processFluid(BrewProcess next, int tickRate, boolean ready) {
        if (next == BrewProcess.IDLE) {
            if (stage != BrewProcess.IDLE) reset(BrewProcess.IDLE, 0);
            return;
        }

        FluidStack contents = getTank().getFluid();
        int ticks = Math.max(1, next.duration(contents));
        if (next != stage) reset(next, ticks);
        else if (duration != ticks) {
            duration = ticks;
            changed();
        }
        if (!ready) return;
        int previous = elapsed;
        elapsed += Math.min(tickRate, duration - elapsed);
        if (elapsed >= duration) {
            if (completeProcess(next, contents)) reset(BrewProcess.IDLE, 0);
        } else if (previous / 20 != elapsed / 20) {
            changed();
        }
    }

    protected boolean completeProcess(BrewProcess process, FluidStack contents) {
        getTank().replace(process.finish(contents));
        if (process == BrewProcess.HARDEN) {
            if (solidOutput.isEmpty()) solidOutput = new ItemStack(Blocks.DIRT);
            else solidOutput.grow(1);
        }
        return true;
    }

    private void reset(BrewProcess next, int ticks) {
        stage = next;
        duration = ticks;
        elapsed = 0;
        changed();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("process", stage.ordinal());
        compound.setInteger("timeProcessed", elapsed);
        compound.setInteger("timeNeeded", duration);
        if (!solidOutput.isEmpty()) compound.setTag("solidContents", solidOutput.writeToNBT(new NBTTagCompound()));
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        FluidStack contents = getTank().getFluid();
        batch = contents == null ? null : contents.copy();
        stage = BrewProcess.fromId(compound.getInteger("process"));
        duration = Math.max(0, compound.getInteger("timeNeeded"));
        elapsed = Math.max(0, Math.min(duration, compound.getInteger("timeProcessed")));
        solidOutput = new ItemStack(compound.getCompoundTag("solidContents"));
    }
}
