/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.screen.FluidMachineMenu;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public abstract class FluidMachineBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    public static final int INPUT=0,OUTPUT=1;
    private static final int[] INPUT_SLOTS={INPUT},OUTPUT_SLOTS={OUTPUT},BOTH={INPUT,OUTPUT},NONE={};
    private final ProcessingTank tank;
    private final NonNullList<ItemStack> vessels=NonNullList.withSize(2,ItemStack.EMPTY);
    private final int[] initialTransfer=new int[2],transferred=new int[2];
    private final EnumMap<Direction,LazyOptional<IItemHandler>> items=new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsided;
    private LazyOptional<IFluidHandler> fluids;
    private int capabilityEpoch;
    private boolean syncPending;
    private CompoundTag itemData=new CompoundTag();
    public final ContainerData data=new ContainerData(){
        @Override public int get(int index){
            FluidProcessingBlockEntity processor=FluidMachineBlockEntity.this instanceof FluidProcessingBlockEntity p?p:null;
            return switch(index){case 0->tank.getFluidAmount();case 1->tank.getCapacity();case 2->processor instanceof MashTubBlockEntity vat&&vat.isStewing()?BrewProcess.values().length:processor==null?0:processor.getStage().ordinal();case 3->processor==null?0:(int)(processor.getProcessingProgress()*1000);
                case 4->processor==null?0:processor.getRemainingTicks()&65535;case 5->processor==null?0:processor.getRemainingTicks()>>>16;
                case 6->(int)(getVesselProgress(INPUT)*1000);case 7->(int)(getVesselProgress(OUTPUT)*1000);case 8->FluidMachineBlockEntity.this instanceof DistilleryBlockEntity d?d.getOutlet().ordinal():0;default->0;};
        }
        @Override public void set(int index,int value){}
        @Override public int getCount(){return 9;}
    };
    protected FluidMachineBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state,int capacity){
        super(type,pos,state);tank=new ProcessingTank(capacity,()->{onTankContentsChanged();changed();});createCapabilities();
    }
    public final ProcessingTank getTank(){return tank;}
    public final float getVesselProgress(int slot){return initialTransfer[slot]>0?(float)transferred[slot]/initialTransfer[slot]:0;}
    private void createCapabilities(){
        final int epoch=capabilityEpoch;
        fluids=LazyOptional.of(()->new IFluidHandler(){
            private boolean active(){return !isRemoved()&&capabilityEpoch==epoch;}
            @Override public int getTanks(){return 1;}
            @Override public FluidStack getFluidInTank(int slot){return active()&&slot==0?tank.getFluid():FluidStack.EMPTY;}
            @Override public int getTankCapacity(int slot){return slot==0?tank.getCapacity():0;}
            @Override public boolean isFluidValid(int slot,FluidStack stack){return active()&&slot==0&&!stack.isEmpty();}
            @Override public int fill(FluidStack stack,FluidAction action){return active()?tank.fill(stack,action):0;}
            @Override public FluidStack drain(FluidStack stack,FluidAction action){return active()?tank.drain(stack,action):FluidStack.EMPTY;}
            @Override public FluidStack drain(int amount,FluidAction action){return active()?tank.drain(amount,action):FluidStack.EMPTY;}
        });
        for(Direction side:Direction.values())items.put(side,LazyOptional.of(()->itemHandler(side,epoch)));
        unsided=LazyOptional.of(()->itemHandler(null,epoch));
    }
    private IItemHandler itemHandler(@Nullable Direction side,int epoch){
        return new SidedInvWrapper(this,side){
            private boolean active(){return !isRemoved()&&capabilityEpoch==epoch;}
            @Override public ItemStack getStackInSlot(int slot){return active()?super.getStackInSlot(slot).copy():ItemStack.EMPTY;}
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return active()?super.insertItem(slot,stack,simulate):stack;}
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return active()?super.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        };
    }
    @Override public void invalidateCaps(){super.invalidateCaps();capabilityEpoch++;fluids.invalidate();unsided.invalidate();items.values().forEach(LazyOptional::invalidate);}
    @Override public void reviveCaps(){super.reviveCaps();createCapabilities();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> type,@Nullable Direction side){
        if(!isRemoved()){
            if(type==ForgeCapabilities.FLUID_HANDLER)return fluids.cast();
            if(type==ForgeCapabilities.ITEM_HANDLER&&(side==null||getSlotsForFace(side).length>0))return(side==null?unsided:items.get(side)).cast();
        }return super.getCapability(type,side);
    }
    public final boolean interactHeld(Player player,InteractionHand hand){
        return transferHeldFluid(player,hand,1000,true)||transferHeldFluid(player,hand,Integer.MAX_VALUE,false);
    }
    protected final boolean transferHeldFluid(Player player,InteractionHand hand,int amount,boolean drainTank){
        var vessel=FluidVesselHandler.forTransfer(player.getItemInHand(hand));if(vessel==null)return false;
        FluidStack moved=drainTank?FluidUtil.tryFluidTransfer(vessel,tank,amount,true):FluidUtil.tryFluidTransfer(tank,vessel,amount,true);
        if(moved.isEmpty())return false;
        if(drainTank||!player.getAbilities().instabuild){
            ItemStack held=player.getItemInHand(hand);if(held.getCount()==1)player.setItemInHand(hand,vessel.getContainer());else{held.shrink(1);ItemHandlerHelper.giveItemToPlayer(player,vessel.getContainer());}
        }
        if(level!=null)level.playSound(null,worldPosition,SoundEvents.BUCKET_FILL,SoundSource.BLOCKS,1,1);return true;
    }
    public final void tick(){
        if(level==null)return;if(level.isClientSide){tickClient();return;}
        boolean moving=transfer(OUTPUT);moving=transfer(INPUT)||moving;
        if(moving&&level.getGameTime()%9==0)level.playSound(null,worldPosition,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,.025F,.5F);
        tickProcessing();
        if(syncPending){syncPending=false;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    }
    private boolean transfer(int slot){
        var vessel=FluidVesselHandler.forTransfer(vessels.get(slot));if(vessel==null)return false;
        FluidStack moved=slot==OUTPUT?FluidUtil.tryFluidTransfer(vessel,tank,50,true):FluidUtil.tryFluidTransfer(tank,vessel,50,true);
        if(moved.isEmpty())return false;
        vessels.set(slot,vessel.getContainer());transferred[slot]+=Math.min(moved.getAmount(),Math.max(0,initialTransfer[slot]-transferred[slot]));changed();return true;
    }
    protected abstract void tickProcessing();
    protected void tickClient(){}
    protected void onTankContentsChanged(){}
    protected final void changed(){if(level!=null&&!level.isClientSide){super.setChanged();syncPending=true;}}
    private void startTransfer(int slot){
        initialTransfer[slot]=transferred[slot]=0;var vessel=FluidVesselHandler.forTransfer(vessels.get(slot));if(vessel==null)return;
        initialTransfer[slot]=slot==INPUT?vessel.drain(Integer.MAX_VALUE,FluidAction.SIMULATE).getAmount():room(vessel);
    }
    private static int room(IFluidHandlerItem vessel){long room=0;for(int i=0;i<vessel.getTanks();i++)room+=Math.max(0,vessel.getTankCapacity(i)-vessel.getFluidInTank(i).getAmount());return (int)Math.min(room,Integer.MAX_VALUE);}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tag.put("tank",tank.writeToNBT(new CompoundTag()));ContainerHelper.saveAllItems(tag,vessels);
        tag.putIntArray("initialTransfer",initialTransfer.clone());tag.putIntArray("transferred",transferred.clone());tag.put("portableItemData",itemData.copy());
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);tank.readFromNBT(tag.getCompound("tank"));vessels.clear();ContainerHelper.loadAllItems(tag,vessels);itemData=tag.getCompound("portableItemData").copy();
        int[] initial=tag.getIntArray("initialTransfer"),moved=tag.getIntArray("transferred");
        for(int slot=0;slot<2;slot++)if(slot<initial.length&&slot<moved.length&&!vessels.get(slot).isEmpty()){initialTransfer[slot]=Math.max(0,initial[slot]);transferred[slot]=Math.max(0,Math.min(initialTransfer[slot],moved[slot]));}else startTransfer(slot);
    }
    public final void placedFrom(ItemStack stack){
        itemData=stack.getTag()==null?new CompoundTag():stack.getTag().copy();itemData.remove("fluid");itemData.remove("BlockEntityTag");
        tank.replace(stack.getTag()==null?FluidStack.EMPTY:FluidContents.read(stack.getTag().getCompound("fluid"),tank.getCapacity()));changed();
    }
    public final ItemStack portableStack(){
        ItemStack stack=new ItemStack(getBlockState().getBlock());CompoundTag tag=itemData.copy();
        if(!tank.isEmpty())tag.put("fluid",FluidContents.write(tank.getFluid()));if(!tag.isEmpty())stack.setTag(tag);return stack;
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){ItemStack stack=portableStack();return stack.hasCustomHoverName()?stack.getHoverName():getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new FluidMachineMenu(id,inventory,this,data,worldPosition);}
    @Override public int getContainerSize(){return 2;}
    @Override public boolean isEmpty(){return vessels.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return vessels.get(slot);}
    @Override public ItemStack removeItem(int slot,int amount){ItemStack result=ContainerHelper.removeItem(vessels,slot,amount);if(!result.isEmpty()){startTransfer(slot);changed();}return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){ItemStack result=ContainerHelper.takeItem(vessels,slot);if(!result.isEmpty()){startTransfer(slot);changed();}return result;}
    @Override public void setItem(int slot,ItemStack stack){if(stack.getCount()>1){stack=stack.copy();stack.setCount(1);}vessels.set(slot,stack);startTransfer(slot);changed();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !isRemoved()&&acceptsVessel(slot,stack);}
    public static boolean acceptsVessel(int slot,ItemStack stack){var vessel=FluidVesselHandler.forTransfer(stack);return vessel!=null&&(slot==INPUT?!vessel.drain(Integer.MAX_VALUE,FluidAction.SIMULATE).isEmpty():slot==OUTPUT&&room(vessel)>0);}
    @Override public int[] getSlotsForFace(@Nullable Direction side){return side==Direction.EAST?INPUT_SLOTS:side==Direction.WEST?OUTPUT_SLOTS:side==null||side.getAxis().isVertical()?BOTH:NONE;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return contains(getSlotsForFace(side),slot)&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){
        if(isRemoved()||!contains(getSlotsForFace(side),slot))return false;var vessel=FluidVesselHandler.forTransfer(stack);
        return vessel!=null&&(slot==INPUT?vessel.drain(Integer.MAX_VALUE,FluidAction.SIMULATE).isEmpty():slot==OUTPUT&&room(vessel)==0);
    }
    private static boolean contains(int[] slots,int slot){for(int value:slots)if(value==slot)return true;return false;}
    @Override public void clearContent(){vessels.clear();for(int slot=0;slot<2;slot++)startTransfer(slot);changed();}
    @Override public boolean stillValid(Player player){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
}
