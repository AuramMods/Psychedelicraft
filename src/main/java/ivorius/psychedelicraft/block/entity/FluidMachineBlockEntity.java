/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.ProcessingTank;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;

import javax.annotation.Nullable;

public abstract class FluidMachineBlockEntity extends TileEntity implements ITickable {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    private static final int TRANSFER_RATE = 50;

    private final ProcessingTank tank;
    private boolean syncPending;
    private final int[] initialTransfer = new int[2];
    private final int[] transferred = new int[2];
    private final VesselSlots vessels = new VesselSlots();

    private final class VesselSlots extends ItemStackHandler {
        private VesselSlots() {
            super(2);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IFluidHandlerItem vessel = FluidVesselHandler.forTransfer(stack);
            return vessel != null && (slot == INPUT ? hasFluid(vessel) : hasRoom(vessel));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (world == null || !world.isRemote) startTransfer(slot);
            changed();
        }

        private void setTransferred(int slot, IFluidHandlerItem vessel, FluidStack moved) {
            stacks.set(slot, vessel.getContainer());
            transferred[slot] += Math.min(moved.amount, initialTransfer[slot] - transferred[slot]);
            changed();
        }

        @Override
        public void deserializeNBT(NBTTagCompound compound) {
            NBTTagCompound fixedSlots = compound.copy();
            fixedSlots.setInteger("Size", 2);
            super.deserializeNBT(fixedSlots);
        }
    }
    private final IItemHandler input = automationSlots(INPUT, OUTPUT);
    private final IItemHandler output = automationSlots(OUTPUT, 2);
    private final IItemHandler both = automationSlots(INPUT, 2);

    protected FluidMachineBlockEntity(int capacity) {
        tank = new ProcessingTank(capacity, () -> {
            onTankContentsChanged();
            changed();
        });
    }

    public final ProcessingTank getTank() {
        return tank;
    }

    public final ItemStackHandler getVessels() {
        return vessels;
    }

    public final float getVesselProgress(int slot) {
        return initialTransfer[slot] > 0 ? (float) transferred[slot] / initialTransfer[slot] : 0;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    public final boolean isUsableByPlayer(EntityPlayer player) {
        return !isInvalid() && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    public final boolean depositHeldFluid(EntityPlayer player, EnumHand hand) {
        if (transferHeldFluid(player, hand, Integer.MAX_VALUE, false) == null) return false;
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1);
        return true;
    }

    @Nullable
    protected final FluidStack transferHeldFluid(EntityPlayer player, EnumHand hand, int amount, boolean drainTank) {
        IFluidHandlerItem vessel = FluidVesselHandler.forTransfer(player.getHeldItem(hand));
        return vessel == null ? null : transferHeldFluid(player, hand, vessel, amount, drainTank);
    }

    @Nullable
    protected final FluidStack transferHeldFluid(EntityPlayer player, EnumHand hand, IFluidHandlerItem vessel,
                                                 int amount, boolean drainTank) {
        FluidStack moved = drainTank ? FluidUtil.tryFluidTransfer(vessel, tank, amount, true)
                : FluidUtil.tryFluidTransfer(tank, vessel, amount, true);
        if (moved == null) return null;
        if (drainTank || !player.capabilities.isCreativeMode) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getCount() == 1) player.setHeldItem(hand, vessel.getContainer());
            else {
                held.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player, vessel.getContainer());
            }
        }
        return moved;
    }

    @Override
    public final void update() {
        if (world.isRemote) {
            tickClient();
            return;
        }
        if (transferVessels() && world.getTotalWorldTime() % 9 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.025F, 0.5F);
        }
        tickProcessing();
        if (syncPending) {
            syncPending = false;
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    protected abstract void tickProcessing();

    protected void onTankContentsChanged() {
    }

    protected void tickClient() {
    }

    private boolean transferVessels() {
        boolean transferred = false;
        IFluidHandlerItem target = FluidVesselHandler.forTransfer(vessels.getStackInSlot(OUTPUT));
        FluidStack filled = target == null ? null : FluidUtil.tryFluidTransfer(target, tank, TRANSFER_RATE, true);
        if (filled != null) {
            vessels.setTransferred(OUTPUT, target, filled);
            transferred = true;
        }
        IFluidHandlerItem source = FluidVesselHandler.forTransfer(vessels.getStackInSlot(INPUT));
        FluidStack drained = source == null ? null : FluidUtil.tryFluidTransfer(tank, source, TRANSFER_RATE, true);
        if (drained != null) {
            vessels.setTransferred(INPUT, source, drained);
            transferred = true;
        }
        return transferred;
    }

    protected final void changed() {
        if (world != null && !world.isRemote) {
            markDirty();
            syncPending = true;
        }
    }

    private IItemHandler automationSlots(int first, int end) {
        return new RangedWrapper(vessels, first, end) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                IFluidHandlerItem vessel = FluidVesselHandler.forTransfer(getStackInSlot(slot));
                if (vessel == null || (slot + first == INPUT ? hasFluid(vessel) : hasRoom(vessel))) {
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    private static boolean hasFluid(IFluidHandlerItem vessel) {
        return vessel.drain(Integer.MAX_VALUE, false) != null;
    }

    private static boolean hasRoom(IFluidHandlerItem vessel) {
        return transferRoom(vessel) > 0;
    }

    private void startTransfer(int slot) {
        transferred[slot] = 0;
        initialTransfer[slot] = 0;
        IFluidHandlerItem vessel = FluidVesselHandler.forTransfer(vessels.getStackInSlot(slot));
        if (vessel == null) return;
        if (slot == INPUT) {
            FluidStack available = vessel.drain(Integer.MAX_VALUE, false);
            if (available != null) initialTransfer[slot] = available.amount;
        } else {
            initialTransfer[slot] = transferRoom(vessel);
        }
    }

    private static int transferRoom(IFluidHandlerItem vessel) {
        long room = 0;
        for (IFluidTankProperties property : vessel.getTankProperties()) {
            if (!property.canFill()) continue;
            FluidStack contents = property.getContents();
            room += Math.max(0, property.getCapacity() - (contents == null ? 0 : contents.amount));
        }
        return (int) Math.min(room, Integer.MAX_VALUE);
    }

    @Nullable
    private IItemHandler automation(@Nullable EnumFacing side) {
        if (side == EnumFacing.EAST) return input;
        if (side == EnumFacing.WEST) return output;
        return side == null || side.getAxis().isVertical() ? both : null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && automation(side) != null)
                || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(tank);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automation(side));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
        compound.setTag("vessels", vessels.serializeNBT());
        compound.setIntArray("initialTransfer", initialTransfer.clone());
        compound.setIntArray("transferred", transferred.clone());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tank.readFromNBT(compound.getCompoundTag("tank"));
        vessels.deserializeNBT(compound.getCompoundTag("vessels"));
        int[] initial = compound.getIntArray("initialTransfer");
        int[] moved = compound.getIntArray("transferred");
        for (int slot = INPUT; slot <= OUTPUT; slot++) {
            if (slot < initial.length && slot < moved.length && !vessels.getStackInSlot(slot).isEmpty()) {
                initialTransfer[slot] = Math.max(0, initial[slot]);
                transferred[slot] = Math.max(0, Math.min(initialTransfer[slot], moved[slot]));
            } else {
                startTransfer(slot);
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }
}
