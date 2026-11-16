/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.recipe.DryingRecipe;
import ivorius.psychedelicraft.recipe.PSRecipes;
import ivorius.psychedelicraft.screen.DryingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.UUID;

public final class DryingTableBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    private static final int[] INPUT_SLOTS={1,2,3,4,5,6,7,8,9};
    private static final int[] OUTPUT_SLOTS={0};
    private static final int[] ALL_SLOTS={0,1,2,3,4,5,6,7,8,9};
    private final NonNullList<ItemStack> items=NonNullList.withSize(10,ItemStack.EMPTY);
    private final EnumMap<Direction,LazyOptional<IItemHandler>> handlers=new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsided;
    private final RecipeManager.CachedCheck<Container,DryingRecipe> recipes=RecipeManager.createCheck(PSRecipes.DRYING_TYPE.get());
    private DryingRecipe recipe;
    private boolean recipeLoaded;
    private ResourceLocation savedRecipe;
    private int ticks;
    private float heat,progress;
    private UUID dealer;
    private long reservationExpires;
    public final ContainerData data=new ContainerData(){
        @Override public int get(int field){return switch(field){case 0->(int)(heat*1000);case 1->(int)(progress*1000);default->0;};}
        @Override public void set(int field,int value){if(field==0)heat=Mth.clamp(value/1000F,0,1);else if(field==1)progress=Mth.clamp(value/1000F,0,1);}
        @Override public int getCount(){return 2;}
    };
    public DryingTableBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.DRYING_TABLE.get(),pos,state);createHandlers();}
    private void createHandlers(){
        for(Direction side:Direction.values())handlers.put(side,LazyOptional.of(()->new SidedInvWrapper(this,side)));
        unsided=LazyOptional.of(()->new SidedInvWrapper(this,null));
    }
    @Override public void invalidateCaps(){super.invalidateCaps();handlers.values().forEach(LazyOptional::invalidate);unsided.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();createHandlers();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side){
        if(!isRemoved()&&capability==ForgeCapabilities.ITEM_HANDLER)return(side==null?unsided:handlers.get(side)).cast();
        return super.getCapability(capability,side);
    }
    public void tick(ServerLevel world){
        if(reservationExpired())release(dealer);
        float oldHeat=heat,oldProgress=progress;
        DryingRecipe matched=recipes.getRecipeFor(this,world).orElse(null);
        if(!recipeLoaded){
            if(savedRecipe!=null&&(matched==null||!savedRecipe.equals(matched.getId())))progress=0;
            recipe=matched;recipeLoaded=true;savedRecipe=null;
        }else if(matched!=recipe){recipe=matched;progress=0;}
        if(++ticks%30==0){
            heat=DryingHeat.at(world.getMaxLocalRawBrightness(worldPosition),world.getBiome(worldPosition).value().getBaseTemperature());
            if(world.getRainLevel(1)>0&&world.getHeight(Heightmap.Types.MOTION_BLOCKING,worldPosition.getX(),worldPosition.getZ())==worldPosition.getY()+1)progress=0;
        }
        if(recipe!=null){
            int duration=getBlockState().is(PSBlocks.IRON_DRYING_TABLE.get())?PSConfig.IRON_DRYING_TABLE_TICKS.get():PSConfig.DRYING_TABLE_TICKS.get();
            progress+=heat/duration;
            int interval=(int)((1-Mth.clamp(progress,0,1))*100);
            if(interval==0||world.getGameTime()%interval==0)smoke(world);
            if(progress>=1){
                // Re-match at the commit point; output and all nine inputs change together.
                DryingRecipe current=recipes.getRecipeFor(this,world).orElse(null);
                if(current==recipe){
                    ItemStack result=recipe.assemble(this,world.registryAccess());
                    NonNullList<ItemStack> remainders=recipe.getRemainingItems(this);
                    items.clear();
                    for(int slot=1;slot<items.size();slot++)items.set(slot,remainders.get(slot));
                    items.set(0,result);
                    inventoryChanged();
                }else{recipe=current;progress=0;}
            }
        }else progress=0;
        if(heat!=oldHeat||progress!=oldProgress){
            super.setChanged();
            if((int)(oldHeat*15)!=(int)(heat*15))world.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
            if((int)(oldProgress*15)!=(int)(progress*15))world.updateNeighborsAt(worldPosition,getBlockState().getBlock());
        }
    }
    private void smoke(ServerLevel world){
        for(int i=0;i<5;i++)world.sendParticles(ParticleTypes.SMOKE,
                worldPosition.getX()+(world.random.nextDouble()+world.random.nextDouble())*.5,
                worldPosition.getY()+.6,worldPosition.getZ()+(world.random.nextDouble()+world.random.nextDouble())*.5,2,0,0,0,0);
    }
    private void inventoryChanged(){
        progress=0;recipeLoaded=false;recipe=null;savedRecipe=null;super.setChanged();
        if(level!=null&&!level.isClientSide){
            level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
        }
    }
    @Override public void setChanged(){inventoryChanged();}
    public float getHeatRatio(){return heat;}
    public float getDryingProgress(){return progress;}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putFloat("heatRatio",heat);tag.putFloat("dryingProgress",progress);
        if(recipe!=null)tag.putString("currentRecipe",recipe.getId().toString());else if(savedRecipe!=null)tag.putString("currentRecipe",savedRecipe.toString());
        if(dealer!=null)tag.putUUID("Dealer",dealer);if(reservationExpires!=0)tag.putLong("ReservationExpires",reservationExpires);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);
        heat=finiteFraction(tag.getFloat("heatRatio"));progress=finiteFraction(tag.getFloat("dryingProgress"));
        savedRecipe=ResourceLocation.tryParse(tag.getString("currentRecipe"));recipe=null;recipeLoaded=false;
        dealer=tag.hasUUID("Dealer")?tag.getUUID("Dealer"):null;reservationExpires=dealer==null?0:tag.getLong("ReservationExpires");
    }
    private static float finiteFraction(float value){return Float.isFinite(value)?Mth.clamp(value,0,1):0;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){ItemStack removed=ContainerHelper.removeItem(items,slot,count);if(!removed.isEmpty())inventoryChanged();return removed;}
    @Override public ItemStack removeItemNoUpdate(int slot){ItemStack removed=ContainerHelper.takeItem(items,slot);if(!removed.isEmpty())inventoryChanged();return removed;}
    @Override public void setItem(int slot,ItemStack stack){
        if(slot!=0&&stack.getCount()>1){stack=stack.copy();stack.setCount(1);}
        if(!ItemStack.matches(items.get(slot),stack)){items.set(slot,stack);inventoryChanged();}
    }
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot>0&&slot<items.size();}
    @Override public int[] getSlotsForFace(@Nullable Direction side){return side==null?ALL_SLOTS:outputFace(side)?OUTPUT_SLOTS:INPUT_SLOTS;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return !outputFace(side)&&canPlaceItem(slot,stack)&&items.get(slot).isEmpty();}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return !outputFace(side)||slot==0;}
    private static boolean outputFace(@Nullable Direction side){return side==Direction.UP||side==Direction.DOWN;}
    @Override public void clearContent(){items.clear();inventoryChanged();}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new DryingTableMenu(id,inventory,this,data,worldPosition);}

    public boolean availableTo(UUID villager){return dealer==null||dealer.equals(villager)||reservationExpired();}
    public boolean reservedFor(UUID villager){return villager.equals(dealer)&&!reservationExpired();}
    public void releaseReservation(UUID villager,long expiresAt){if(expiresAt!=0&&expiresAt==reservationExpires)release(villager);}
    public boolean reserve(UUID villager,long expiresAt){
        if(!availableTo(villager))return false;
        if(!reservedFor(villager)){dealer=villager;reservationExpires=expiresAt;super.setChanged();}return true;
    }
    private boolean reservationExpired(){return reservationExpires!=0&&level!=null&&level.getGameTime()>=reservationExpires;}
    public boolean claim(UUID villager){
        if(!availableTo(villager))return false;
        if(!villager.equals(dealer)||reservationExpires!=0){dealer=villager;reservationExpires=0;super.setChanged();}return true;
    }
    public void release(UUID villager){if(villager.equals(dealer)){dealer=null;reservationExpires=0;super.setChanged();}}
}
