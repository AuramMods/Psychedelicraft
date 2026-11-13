/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public final class BottleRackBlockEntity extends DrinkDisplayBlockEntity {
    public static final int SIZE = 9;

    private final ItemStackHandler bottles = new ItemStackHandler(SIZE) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == PSItems.BOTTLE || stack.getItem() == PSItems.MOLOTOV_COCKTAIL;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };

    public ItemStackHandler getBottles() {
        return bottles;
    }

    public void dropContents() {
        for (int slot = 0; slot < SIZE; slot++) {
            Block.spawnAsEntity(world, pos, bottles.getStackInSlot(slot));
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                ? side == null || side.getAxis().isHorizontal() : super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return side == null || side.getAxis().isHorizontal() ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(bottles) : null;
        }
        return super.getCapability(capability, side);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("bottles", bottles.serializeNBT());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagCompound saved = tag.getCompoundTag("bottles").copy();
        saved.setInteger("Size", SIZE);
        bottles.deserializeNBT(saved);
    }
}
