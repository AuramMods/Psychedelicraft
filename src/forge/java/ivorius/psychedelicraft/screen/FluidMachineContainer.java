/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.screen;

import ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class FluidMachineContainer extends Container {
    private static final int PLAYER_START = 2;
    private static final int END = PLAYER_START + 36;

    private final FluidMachineBlockEntity machine;

    public FluidMachineContainer(InventoryPlayer inventory, FluidMachineBlockEntity machine) {
        this.machine = machine;
        addSlotToContainer(new SlotItemHandler(machine.getVessels(), FluidMachineBlockEntity.INPUT, 21, 20));
        addSlotToContainer(new SlotItemHandler(machine.getVessels(), FluidMachineBlockEntity.OUTPUT, 123, 61));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(inventory, x, 8 + x * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return machine.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack original = slot.getStack().copy();
        ItemStack remaining = original.copy();
        boolean moved = index < PLAYER_START ? mergeItemStack(remaining, PLAYER_START, END, true)
                : mergeItemStack(remaining, 0, PLAYER_START, false);
        if (!moved || remaining.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.putStack(remaining);
        slot.onTake(player, remaining);
        return original;
    }
}
