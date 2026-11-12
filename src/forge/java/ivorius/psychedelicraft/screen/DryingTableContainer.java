/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.screen;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import static ivorius.psychedelicraft.recipe.DryingRecipe.INPUT_COUNT;
import static ivorius.psychedelicraft.recipe.DryingRecipe.OUTPUT;

public final class DryingTableContainer extends Container {
    private static final int PLAYER_START = INPUT_COUNT + 1;
    private static final int END = PLAYER_START + 36;

    private final DryingTableBlockEntity table;
    private final int[] fields;

    public DryingTableContainer(InventoryPlayer inventory, DryingTableBlockEntity table) {
        this.table = table;
        this.fields = new int[table.getFieldCount()];
        addSlotToContainer(new ResultSlot(table));
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                addSlotToContainer(new Slot(table, 1 + x * 3 + y, 30 + x * 18, 17 + y * 18));
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(inventory, 9 + y * 9 + x, 8 + x * 18, 84 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(inventory, x, 8 + x * 18, 142));
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, table);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int field = 0; field < fields.length; field++) {
            int current = table.getField(field);
            if (current != fields[field]) {
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, field, current);
                }
                fields[field] = current;
            }
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        table.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return table.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getStack().copy();
        ItemStack stack = original.copy();
        if (index < PLAYER_START) {
            mergeItemStack(stack, PLAYER_START, END, index == OUTPUT);
        } else {
            while (!stack.isEmpty() && mergeItemStack(stack, 1, PLAYER_START, false)) {
                // Each empty input slot accepts one item.
            }
        }

        int moved = original.getCount() - stack.getCount();
        if (moved == 0) {
            return ItemStack.EMPTY;
        }
        slot.putStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onSlotChange(stack, original);
        slot.onTake(player, original);
        return original;
    }

    private static final class ResultSlot extends Slot {
        private int removed;

        ResultSlot(DryingTableBlockEntity table) {
            super(table, OUTPUT, 124, 35);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            ItemStack stack = super.decrStackSize(amount);
            removed += stack.getCount();
            return stack;
        }

        @Override
        protected void onCrafting(ItemStack stack, int amount) {
            removed += amount;
        }

        @Override
        protected void onSwapCraft(int amount) {
            removed += amount;
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            stack.onCrafting(player.world, player, removed);
            if (!player.world.isRemote && removed > 0) {
                player.world.spawnEntity(new EntityXPOrb(player.world, player.posX, player.posY, player.posZ, removed));
            }
            removed = 0;
            return super.onTake(player, stack);
        }
    }
}
