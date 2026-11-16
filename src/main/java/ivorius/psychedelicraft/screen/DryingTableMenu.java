/* Copyright (c) 2014, Lukas Tenbrink. Target-version layout by Sollace. */
package ivorius.psychedelicraft.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class DryingTableMenu extends AbstractContainerMenu {
    private static final int PLAYER_START=10,END=46;
    private final Container table;
    private final ContainerData data;
    private final BlockPos position;
    public DryingTableMenu(int id,Inventory inventory,FriendlyByteBuf buffer){
        this(id,inventory,new SimpleContainer(10),new SimpleContainerData(2),buffer.readBlockPos());
    }
    public DryingTableMenu(int id,Inventory inventory,Container table,ContainerData data,BlockPos position){
        super(PSMenus.DRYING_TABLE.get(),id);checkContainerSize(table,10);checkContainerDataCount(data,2);
        this.table=table;this.data=data;this.position=position;
        addSlot(new ResultSlot(table));
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)addSlot(new Slot(table,1+x*3+y,30+x*18,17+y*18){
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+y*9+x,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));
        addDataSlots(data);
    }
    public BlockPos getPosition(){return position;}
    public float getHeatRatio(){return data.get(0)/1000F;}
    public float getProgress(){return data.get(1)/1000F;}
    @Override public boolean stillValid(Player player){return table.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack original=slot.getItem().copy(),remaining=original.copy();
        if(index<PLAYER_START)moveItemStackTo(remaining,PLAYER_START,END,index==0);
        else while(!remaining.isEmpty()&&moveItemStackTo(remaining,1,PLAYER_START,false)){/* One item per empty input slot. */}
        if(remaining.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.set(remaining.isEmpty()?ItemStack.EMPTY:remaining);
        slot.onQuickCraft(remaining,original);slot.onTake(player,original);
        return original;
    }
    private static final class ResultSlot extends Slot {
        private int removed;
        ResultSlot(Container table){super(table,0,124,35);}
        @Override public boolean mayPlace(ItemStack stack){return false;}
        @Override public int getMaxStackSize(){return 64;}
        @Override public ItemStack remove(int count){ItemStack stack=super.remove(count);removed+=stack.getCount();return stack;}
        @Override protected void onQuickCraft(ItemStack stack,int count){removed+=count;}
        @Override protected void onSwapCraft(int count){removed+=count;}
        @Override public void onTake(Player player,ItemStack stack){
            stack.onCraftedBy(player.level(),player,removed);
            if(player.level() instanceof ServerLevel server&&removed>0)ExperienceOrb.award(server,player.position(),removed);
            removed=0;super.onTake(player,stack);
        }
    }
}
