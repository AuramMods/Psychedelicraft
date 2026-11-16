/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.BrewProcess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class FluidProcessingBlockEntity extends FluidMachineBlockEntity {
    private BrewProcess stage = BrewProcess.IDLE;
    private FluidStack batch=FluidStack.EMPTY;
    private int elapsed;
    private int duration;
    private ItemStack solidOutput = ItemStack.EMPTY;

    protected FluidProcessingBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state,int capacity) {
        super(type,pos,state,capacity);
    }

    public final BrewProcess getStage() {
        return stage;
    }

    public int getRemainingTicks() {
        return Math.max(0, duration - elapsed);
    }

    public float getProcessingProgress() {
        return duration > 0 ? (float) Math.min(elapsed, duration) / duration : 0;
    }

    public final ItemStack getSolidOutput() {
        return solidOutput.copy();
    }

    public final boolean collectSolidOutput(Player player) {
        ItemStack output = takeSolidOutput();
        if (output.isEmpty()) return false;
        ItemHandlerHelper.giveItemToPlayer(player, output);
        return true;
    }

    public final ItemStack removeSolidOutput(){return takeSolidOutput();}

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
        if (!contents.isFluidEqual(batch)) {
            batch = contents.copy();
            reset(BrewProcess.IDLE, 0);
        }
    }

    protected final void processFluid(boolean aging) {
        FluidStack contents = getTank().getFluid();
        BrewProcess next = BrewProcess.find(contents, aging, solidOutput.getCount() < 64);
        processFluid(next, next == BrewProcess.IDLE ? 0 : next.duration(contents), true);
    }

    protected final void processFluid(BrewProcess next, int requiredTicks, boolean ready) {
        if (next == BrewProcess.IDLE) {
            if (stage != BrewProcess.IDLE) reset(BrewProcess.IDLE, 0);
            return;
        }

        FluidStack contents = getTank().getFluid();
        int ticks = Math.max(1, requiredTicks);
        if (next != stage) reset(next, ticks);
        else if (duration != ticks) {
            duration = ticks;
            changed();
        }
        if (!ready) return;
        int previous = elapsed;
        if (elapsed < duration) elapsed++;
        // Persist every tick without sending a tile update for each increment.
        if (elapsed != previous) super.setChanged();
        if (elapsed >= duration) {
            if (completeProcess(next, contents)) reset(BrewProcess.IDLE, 0);
        } else if (previous / 20 != elapsed / 20) {
            changed();
        }
    }

    protected final void rescaleProcessingProgress(int requiredTicks) {
        if (duration <= 0 || requiredTicks <= 0) return;
        elapsed = (int) ((long) Math.min(elapsed, duration) * requiredTicks / duration);
        changed();
    }

    protected final void clearProcessingProgress() {
        if (elapsed == 0) return;
        elapsed = 0;
        changed();
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

    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tag.putInt("process",stage.ordinal());tag.putInt("timeProcessed",elapsed);tag.putInt("timeNeeded",duration);
        if(!solidOutput.isEmpty())tag.put("solidContents",solidOutput.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);batch=getTank().getFluid();stage=BrewProcess.fromId(tag.getInt("process"));duration=Math.max(0,tag.getInt("timeNeeded"));
        elapsed=Math.max(0,tag.getInt("timeProcessed"));solidOutput=ItemStack.of(tag.getCompound("solidContents"));
    }
}
