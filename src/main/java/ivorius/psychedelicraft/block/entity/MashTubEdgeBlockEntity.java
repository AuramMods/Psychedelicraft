/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block.entity;
import ivorius.psychedelicraft.block.MashTubBlock;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Proxy handles expire with both the edge and the particular owning tank. */
public final class MashTubEdgeBlockEntity extends BlockEntity {
    private final Map<Key,LazyOptional<?>> handles=new HashMap<>();
    private int epoch;
    public MashTubEdgeBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.MASH_TUB_EDGE.get(),pos,state);}
    private record Key(Capability<?> capability,Direction side){}
    private boolean active(MashTubBlockEntity owner,int captured){return !isRemoved()&&epoch==captured&&MashTubBlock.getVat(level,worldPosition)==owner;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side){
        if(!isRemoved()&&(capability==ForgeCapabilities.FLUID_HANDLER||capability==ForgeCapabilities.ITEM_HANDLER)){
            var owner=MashTubBlock.getVat(level,worldPosition);if(owner==null)return LazyOptional.empty();Key key=new Key(capability,side);
            var cached=handles.get(key);if(cached!=null)return cached.cast();int captured=epoch;
            LazyOptional<T> original=owner.getCapability(capability,side);if(!original.isPresent())return LazyOptional.empty();
            LazyOptional<?> wrapped;
            if(capability==ForgeCapabilities.FLUID_HANDLER){IFluidHandler target=(IFluidHandler)original.orElseThrow(IllegalStateException::new);wrapped=LazyOptional.of(()->new IFluidHandler(){
                private boolean valid(){return original.isPresent()&&active(owner,captured);}
                public int getTanks(){return 1;}public FluidStack getFluidInTank(int tank){return valid()?target.getFluidInTank(tank):FluidStack.EMPTY;}
                public int getTankCapacity(int tank){return valid()?target.getTankCapacity(tank):0;}public boolean isFluidValid(int tank,FluidStack stack){return valid()&&target.isFluidValid(tank,stack);}
                public int fill(FluidStack stack,FluidAction action){return valid()?target.fill(stack,action):0;}public FluidStack drain(FluidStack stack,FluidAction action){return valid()?target.drain(stack,action):FluidStack.EMPTY;}public FluidStack drain(int amount,FluidAction action){return valid()?target.drain(amount,action):FluidStack.EMPTY;}
            });}else{IItemHandler target=(IItemHandler)original.orElseThrow(IllegalStateException::new);wrapped=LazyOptional.of(()->new IItemHandler(){
                private boolean valid(){return original.isPresent()&&active(owner,captured);}
                public int getSlots(){return target.getSlots();}public ItemStack getStackInSlot(int slot){return valid()?target.getStackInSlot(slot):ItemStack.EMPTY;}
                public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return valid()?target.insertItem(slot,stack,simulate):stack;}
                public ItemStack extractItem(int slot,int amount,boolean simulate){return valid()?target.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
                public int getSlotLimit(int slot){return valid()?target.getSlotLimit(slot):0;}public boolean isItemValid(int slot,ItemStack stack){return valid()&&target.isItemValid(slot,stack);}
            });}
            handles.put(key,wrapped);original.addListener(ignored->{wrapped.invalidate();handles.remove(key,wrapped);});return wrapped.cast();
        }return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();epoch++;List.copyOf(handles.values()).forEach(LazyOptional::invalidate);handles.clear();}
}
