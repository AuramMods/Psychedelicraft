/* Copyright (c) 2014, Lukas Tenbrink. Native layout by Sollace. */
package ivorius.psychedelicraft.screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import ivorius.psychedelicraft.fluid.BrewProcess;

public final class FluidMachineMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    private final BlockPos position;
    public FluidMachineMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,new SimpleContainer(2),new SimpleContainerData(9),buffer.readBlockPos());}
    public FluidMachineMenu(int id,Inventory inventory,Container machine,ContainerData data,BlockPos position){
        super(PSMenus.FLUID_MACHINE.get(),id);checkContainerSize(machine,2);checkContainerDataCount(data,9);this.machine=machine;this.data=data;this.position=position;
        for(int slot=0;slot<2;slot++){final int index=slot;addSlot(new Slot(machine,slot,slot==0?21:123,slot==0?20:61){
            @Override public int getMaxStackSize(){return 1;}
            @Override public boolean mayPlace(ItemStack stack){return ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity.acceptsVessel(index,stack)&&machine.canPlaceItem(index,stack);}
        });}
        for(int row=0;row<3;row++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+row*9+x,8+x*18,84+row*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));addDataSlots(data);
    }
    public BlockPos getPosition(){return position;}
    public int getFluidAmount(){return data.get(0);}
    public int getCapacity(){return data.get(1);}
    public BrewProcess getStage(){return BrewProcess.fromId(data.get(2));}
    public float getProgress(){return data.get(3)/1000F;}
    public int getRemainingTicks(){return data.get(4)&65535|data.get(5)<<16;}
    public float getVesselProgress(int slot){return data.get(slot==0?6:7)/1000F;}
    public int getOutlet(){return data.get(8);}
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;ItemStack original=slot.getItem().copy(),remaining=original.copy();
        if(index<2)moveItemStackTo(remaining,2,38,true);else moveItemStackTo(remaining,0,2,false);
        if(remaining.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.set(remaining.isEmpty()?ItemStack.EMPTY:remaining);slot.onTake(player,original);return original;
    }
}
