/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.FluidAttributesBlockEntity;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
/** A native liquid block with an optional synchronized batch-data block entity. */
public final class PhysicalFluidBlock extends LiquidBlock implements EntityBlock,IFluidBlock {
    public static final BooleanProperty ATTRIBUTED=BooleanProperty.create("attributed");
    public final PSFluids.Family family;
    public PhysicalFluidBlock(PSFluids.Family family){super(family.source,Properties.copy(Blocks.WATER));this.family=family;registerDefaultState(defaultBlockState().setValue(LEVEL,0).setValue(ATTRIBUTED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(ATTRIBUTED);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(ATTRIBUTED)?new FluidAttributesBlockEntity(pos,state):null;}
    @Override public void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){if(old.is(this)&&(!next.is(this)||!next.getValue(ATTRIBUTED)))level.removeBlockEntity(pos);super.onRemove(old,level,pos,next,moving);}
    public FluidStack contents(BlockGetter level,BlockPos pos){
        if(!level.getBlockState(pos).is(this))return FluidStack.EMPTY;
        var attributes=level.getBlockEntity(pos) instanceof FluidAttributesBlockEntity entity?entity.attributes():new CompoundTag();
        return new FluidStack(family.get(),1000,attributes.isEmpty()?null:attributes);
    }
    public static CompoundTag neighborAttributes(BlockGetter level,BlockPos pos,Fluid fluid){
        // Accepted donor order: first matching neighbor, including an untagged or flowing one.
        for(var side:Direction.values()){
            var next=pos.relative(side);if(level instanceof Level world&&!world.hasChunkAt(next))continue;
            if(level.getBlockState(next).getBlock() instanceof PhysicalFluidBlock block&&fluid.isSame(block.getFluid())){var stack=block.contents(level,next);return stack.hasTag()?stack.getTag().copy():new CompoundTag();}
        }return new CompoundTag();
    }
    public boolean setFluid(LevelAccessor level,BlockPos pos,FluidState fluid,CompoundTag attributes){
        boolean tagged=attributes!=null&&!attributes.isEmpty();var next=fluid.createLegacyBlock().setValue(ATTRIBUTED,tagged);var previous=level.getBlockState(pos);
        if(previous!=next&&!level.setBlock(pos,next,Block.UPDATE_ALL))return false;
        if(tagged){if(!(level.getBlockEntity(pos) instanceof FluidAttributesBlockEntity))return false;boolean changed=((FluidAttributesBlockEntity)level.getBlockEntity(pos)).setAttributes(attributes);if(changed&&previous==next&&level instanceof Level world&&!world.isClientSide)world.updateNeighborsAt(pos,this);}
        else if(level instanceof Level world&&level.getBlockEntity(pos) instanceof FluidAttributesBlockEntity)world.removeBlockEntity(pos);
        return true;
    }
    @Override public FlowingFluid getFluid(){return family.get();}
    public boolean canPlace(Level level,BlockPos pos){
        if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunkAt(pos))return false;
        var target=level.getBlockState(pos);var fluid=target.getFluidState();
        if(!fluid.isEmpty())return family.get().isSame(fluid.getType())&&!fluid.isSource()&&target.is(this);
        return !(target.getBlock() instanceof LiquidBlockContainer)&&(!target.isSolid()||target.canBeReplaced());
    }
    @Override public int place(Level level,BlockPos pos,FluidStack supplied,FluidAction action){
        if(PSFluids.canonical(supplied.getFluid())!=family.get()||supplied.getAmount()<1000||!canPlace(level,pos))return 0;
        if(action.execute()&&!level.isClientSide){FluidUtil.destroyBlockOnFluidPlacement(level,pos);if(!setFluid(level,pos,family.get().defaultFluidState(),supplied.getTag()))return 0;}
        return 1000;
    }
    @Override public boolean canDrain(Level level,BlockPos pos){return level.hasChunkAt(pos)&&level.getBlockState(pos).is(this)&&level.getFluidState(pos).isSource();}
    @Override public FluidStack drain(Level level,BlockPos pos,FluidAction action){if(!canDrain(level,pos))return FluidStack.EMPTY;var result=contents(level,pos);return action.execute()&&!level.isClientSide&&!level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL)?FluidStack.EMPTY:result;}
    @Override public ItemStack pickupBlock(LevelAccessor level,BlockPos pos,BlockState state){
        if(!state.is(this)||state.getValue(LEVEL)!=0)return ItemStack.EMPTY;var fluid=contents(level,pos);
        if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return ItemStack.EMPTY;
        return fluid.getFluid().getFluidType().getBucket(fluid);
    }
    @Override public float getFilledPercentage(Level level,BlockPos pos){return level.getBlockState(pos).is(this)?level.getFluidState(pos).getHeight(level,pos):0;}
}
