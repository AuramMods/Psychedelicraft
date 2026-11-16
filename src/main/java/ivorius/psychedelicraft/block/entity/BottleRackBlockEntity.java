/* Copyright (c) 2014, Lukas Tenbrink. Native inventory design by Sollace. */
package ivorius.psychedelicraft.block.entity;
import ivorius.psychedelicraft.PSTags;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;
public final class BottleRackBlockEntity extends DrinkDisplayBlockEntity implements WorldlyContainer {
    public static final int SIZE=9;
    private static final int[] SLOTS={0,1,2,3,4,5,6,7,8},NONE={};
    private final NonNullList<ItemStack> bottles=NonNullList.withSize(SIZE,ItemStack.EMPTY);
    private LazyOptional<IItemHandler> items;private int epoch;
    public BottleRackBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.BOTTLE_RACK.get(),pos,state);createCapability();}
    private void createCapability(){int captured=epoch;items=LazyOptional.of(()->new SidedInvWrapper(this,Direction.NORTH){
        private boolean valid(){return !isRemoved()&&captured==epoch;}
        @Override public ItemStack getStackInSlot(int slot){return valid()?super.getStackInSlot(slot).copy():ItemStack.EMPTY;}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return valid()?super.insertItem(slot,stack,simulate):stack;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return valid()?super.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
    });}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> type,@Nullable Direction side){if(!isRemoved()&&type==ForgeCapabilities.ITEM_HANDLER&&(side==null||side.getAxis().isHorizontal()))return items.cast();return super.getCapability(type,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();epoch++;items.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();createCapability();}
    @Override public int getContainerSize(){return SIZE;}
    @Override public boolean isEmpty(){return bottles.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return bottles.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){ItemStack result=ContainerHelper.removeItem(bottles,slot,count);if(!result.isEmpty())contentsChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){ItemStack result=ContainerHelper.takeItem(bottles,slot);if(!result.isEmpty())contentsChanged();return result;}
    @Override public void setItem(int slot,ItemStack stack){bottles.set(slot,stack.copyWithCount(Math.min(1,stack.getCount())));contentsChanged();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !isRemoved()&&stack.is(PSTags.BOTTLES);}
    @Override public int[] getSlotsForFace(Direction face){return face==null||face.getAxis().isHorizontal()?SLOTS:NONE;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return (face==null||face.getAxis().isHorizontal())&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return !isRemoved()&&(face==null||face.getAxis().isHorizontal());}
    @Override public boolean stillValid(Player player){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){bottles.clear();contentsChanged();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,bottles);}
    @Override public void load(CompoundTag tag){super.load(tag);bottles.clear();ContainerHelper.loadAllItems(tag,bottles);}
}
