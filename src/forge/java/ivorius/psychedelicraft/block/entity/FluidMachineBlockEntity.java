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
    private final ItemStackHandler vessels = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IFluidHandlerItem vessel = itemHandler(stack);
            return vessel != null && (slot == INPUT ? hasFluid(vessel) : hasRoom(vessel));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    private final IItemHandler input = automationSlots(INPUT, OUTPUT);
    private final IItemHandler output = automationSlots(OUTPUT, 2);
    private final IItemHandler both = automationSlots(INPUT, 2);

    protected FluidMachineBlockEntity(int capacity) {
        tank = new ProcessingTank(capacity, this::changed);
    }

    public final ProcessingTank getTank() {
        return tank;
    }

    public final ItemStackHandler getVessels() {
        return vessels;
    }

    public final boolean isUsableByPlayer(EntityPlayer player) {
        return !isInvalid() && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    public final boolean depositHeldFluid(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        IFluidHandlerItem vessel = itemHandler(held);
        if (vessel == null || FluidUtil.tryFluidTransfer(tank, vessel, Integer.MAX_VALUE, true) == null) return false;
        if (!player.capabilities.isCreativeMode) {
            ItemStack remainder = vessel.getContainer();
            if (held.getCount() == 1) {
                player.setHeldItem(hand, remainder);
            } else {
                held.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player, remainder);
            }
        }
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1, 1);
        return true;
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

    protected void tickClient() {
    }

    private boolean transferVessels() {
        boolean transferred = false;
        IFluidHandlerItem target = itemHandler(vessels.getStackInSlot(OUTPUT));
        if (target != null && FluidUtil.tryFluidTransfer(target, tank, TRANSFER_RATE, true) != null) {
            vessels.setStackInSlot(OUTPUT, target.getContainer());
            transferred = true;
        }
        IFluidHandlerItem source = itemHandler(vessels.getStackInSlot(INPUT));
        if (source != null && FluidUtil.tryFluidTransfer(tank, source, TRANSFER_RATE, true) != null) {
            vessels.setStackInSlot(INPUT, source.getContainer());
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
                IFluidHandlerItem vessel = itemHandler(getStackInSlot(slot));
                if (vessel == null || (slot + first == INPUT ? hasFluid(vessel) : hasRoom(vessel))) {
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    @Nullable
    public static IFluidHandlerItem itemHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        FluidVesselHandler vessel = FluidVesselHandler.forCrafting(stack);
        if (vessel != null) return vessel;
        ItemStack single = stack.copy();
        single.setCount(1);
        return FluidUtil.getFluidHandler(single);
    }

    private static boolean hasFluid(IFluidHandlerItem vessel) {
        return vessel.drain(Integer.MAX_VALUE, false) != null;
    }

    private static boolean hasRoom(IFluidHandlerItem vessel) {
        for (IFluidTankProperties property : vessel.getTankProperties()) {
            FluidStack contents = property.getContents();
            if (property.canFill() && (contents == null || contents.amount < property.getCapacity())) return true;
        }
        return false;
    }

    public static float fillRatio(ItemStack stack) {
        IFluidHandlerItem vessel = itemHandler(stack);
        if (vessel == null) return 0;
        for (IFluidTankProperties property : vessel.getTankProperties()) {
            FluidStack contents = property.getContents();
            if (contents != null && property.getCapacity() > 0) return (float) contents.amount / property.getCapacity();
        }
        return 0;
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
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tank.readFromNBT(compound.getCompoundTag("tank"));
        vessels.deserializeNBT(compound.getCompoundTag("vessels"));
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
