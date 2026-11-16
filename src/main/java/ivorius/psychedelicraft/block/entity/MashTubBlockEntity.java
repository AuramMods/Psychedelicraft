/* Copyright (c) 2014, Lukas Tenbrink. Native design by Sollace. */
package ivorius.psychedelicraft.block.entity;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.particle.BubbleParticleOptions;
import ivorius.psychedelicraft.recipe.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;

public final class MashTubBlockEntity extends FluidProcessingBlockEntity {
    public static final int CAPACITY=32000;
    private final List<ItemStack> supplied=new ArrayList<>();
    private ResourceLocation activeRecipe;
    private int pulsesLeft,totalPulses,lastLight;
    private boolean dismantling;
    public MashTubBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.MASH_TUB.get(),pos,state,CAPACITY);}
    public List<ItemStack> getSuppliedIngredients(){return supplied.stream().map(ItemStack::copy).toList();}
    public List<ItemStack> takeSuppliedIngredients(){List<ItemStack> result=getSuppliedIngredients();supplied.clear();changed();return result;}
    public boolean isStewing(){return activeRecipe!=null;}
    public boolean isDismantling(){return dismantling;}
    public void beginDismantling(){dismantling=true;getTank().setCanFill(false);getTank().setCanDrain(false);invalidateCaps();}
    public float getStewProgress(){return totalPulses>0?1-pulsesLeft/(float)totalPulses:0;}
    public float getFluidSurface(){return getTank().isEmpty()?0:.3F+.6F*getTank().getFluidAmount()/getTank().getCapacity();}
    @Override public float getProcessingProgress(){return isStewing()?getStewProgress():super.getProcessingProgress();}
    @Override public int getRemainingTicks(){return isStewing()?(int)Math.min(Integer.MAX_VALUE,30L*pulsesLeft):super.getRemainingTicks();}
    public boolean interactWithVessel(Player player,InteractionHand hand){
        var vessel=FluidVesselHandler.forTransfer(player.getItemInHand(hand));if(vessel==null)return false;
        boolean withdraw=vessel.drain(Integer.MAX_VALUE,FluidAction.SIMULATE).isEmpty();FluidStack before=getTank().getFluid();
        if(!transferHeldFluid(player,hand,Integer.MAX_VALUE,withdraw))return false;
        if(withdraw){before.setAmount(before.getAmount()-getTank().getFluidAmount());PSCriteria.tookMash(player,before);}return true;
    }
    public boolean dropSolidOutput(Player player){ItemStack output=takeSolidOutput();if(output.isEmpty())return false;PSCriteria.tookMash(player,output);Block.popResource(level,worldPosition,output);return true;}
    public boolean addIngredient(ItemStack held){
        FluidStack contents=getTank().getFluid();if(isStewing()||held.isEmpty()||contents.isEmpty()||FluidVesselHandler.forTransfer(held)!=null)return false;
        List<MashingRecipe> candidates=level.getRecipeManager().getAllRecipesFor(PSRecipes.MASHING_TYPE.get()).stream().filter(recipe->recipe.matchesPool(contents)).toList();
        if(candidates.stream().noneMatch(recipe->recipe.acceptsIngredient(held)))return false;
        ItemStack ingredient=held.copy();ingredient.setCount(1);supplied.add(ingredient);int matches=0;MashingRecipe complete=null;
        for(var recipe:candidates)if(recipe.matchesPartial(supplied)){matches++;if(recipe.matchesIngredients(supplied))complete=recipe;}
        if(matches==0){supplied.clear();getTank().replace(new FluidStack(PSFluids.SLURRY.get(),contents.getAmount(),contents.getTag()));bubbles(90,.5F,SoundEvents.GRAVEL_BREAK);}
        else if(matches==1&&complete!=null){activeRecipe=complete.getId();pulsesLeft=totalPulses=2+level.random.nextInt(4)+complete.getStewTime();getTank().setCanFill(false);getTank().setCanDrain(false);}
        bubbles(20,0,SoundEvents.LAVA_POP);level.playSound(null,worldPosition,SoundEvents.GENERIC_SPLASH,SoundSource.BLOCKS,1,1);changed();return true;
    }
    @Override protected void tickProcessing(){
        updateLight();if(!isStewing()){processFluid(false);return;}if(level.getGameTime()%30!=0)return;
        var entry=level.getRecipeManager().byKey(activeRecipe).orElse(null);
        if(!(entry instanceof MashingRecipe recipe)||!recipe.matchesPool(getTank().getFluid())||!recipe.matchesIngredients(supplied)){endStew();return;}
        bubbles(9,.5F,SoundEvents.LAVA_AMBIENT);
        if(--pulsesLeft<=0){getTank().replace(recipe.finish(getTank().getFluidAmount()));supplied.clear();endStew();}else changed();
    }
    private void endStew(){activeRecipe=null;pulsesLeft=totalPulses=0;getTank().setCanFill(true);getTank().setCanDrain(true);changed();}
    @Override protected void tickClient(){
        if(!getTank().isEmpty()&&!supplied.isEmpty()&&level.getGameTime()%3==0&&level.random.nextFloat()<.33F){
            Vec3 center=bubbleOrigin();int count=1+(int)(supplied.size()*1.5);double vx=triangular(.125),vy=.1+triangular(.125),vz=triangular(.125);
            for(int i=0;i<count;i++)level.addParticle(new BubbleParticleOptions(color(),1),center.x+triangular(.5),center.y+triangular(.5),center.z+triangular(.5),vx,vy,vz);
            level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,SoundEvents.LAVA_POP,SoundSource.BLOCKS,.5F+level.random.nextFloat(),.3F+level.random.nextFloat(),true);
        }
    }
    private int color(){var fluid=getTank().getFluid();return fluid.getFluid()==net.minecraft.world.level.material.Fluids.WATER?0xff3f76e4:PSFluids.color(fluid);}
    private double triangular(double radius){return(level.random.nextDouble()-level.random.nextDouble())*radius;}
    private Vec3 bubbleOrigin(){return new Vec3(worldPosition.getX()+.5+triangular(.25),worldPosition.getY()+1+triangular(.25),worldPosition.getZ()+.5+triangular(.25));}
    private void bubbles(int count,float spread,SoundEvent sound){
        if(getTank().isEmpty())return;Vec3 center=bubbleOrigin();double vx=triangular(.125),vy=.1+triangular(.125),vz=triangular(.125);
        for(int i=0;i<count;i++)((ServerLevel)level).sendParticles(new BubbleParticleOptions(color(),1),center.x+triangular(.5+spread),center.y+triangular(.5+spread),center.z+triangular(.5+spread),0,vx,vy,vz,1);
        level.playSound(null,worldPosition,sound,SoundSource.BLOCKS,.5F+level.random.nextFloat(),.3F+level.random.nextFloat());
    }
    private void updateLight(){
        var fluid=getTank().getFluid();int light=fluid.isEmpty()?0:fluid.getFluid().getFluidType().getLightLevel(fluid);if(light==lastLight)return;lastLight=light;
        for(BlockPos part:BlockPos.betweenClosed(worldPosition.offset(-1,0,-1),worldPosition.offset(1,0,1))){
            BlockState state=level.getBlockState(part);if(state.getBlock() instanceof ivorius.psychedelicraft.block.MashTubBlock||state.getBlock() instanceof ivorius.psychedelicraft.block.MashTubWallBlock)level.setBlock(part,state.setValue(ivorius.psychedelicraft.block.MashTubBlock.LIGHT,light),Block.UPDATE_CLIENTS);
        }
    }
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition.offset(-1,0,-1),worldPosition.offset(2,2,2));}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);ListTag items=new ListTag();for(ItemStack item:supplied)items.add(item.save(new CompoundTag()));tag.put("suppliedIngredients",items);
        if(activeRecipe!=null){tag.putString("stewRecipe",activeRecipe.toString());tag.putInt("pulsesLeft",pulsesLeft);tag.putInt("totalPulses",totalPulses);}
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);supplied.clear();ListTag items=tag.getList("suppliedIngredients",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(items.size(),MashingRecipe.MAX_INGREDIENTS);i++){ItemStack item=ItemStack.of(items.getCompound(i));if(!item.isEmpty()){item.setCount(1);supplied.add(item);}}
        pulsesLeft=tag.getInt("pulsesLeft");totalPulses=Math.max(pulsesLeft,tag.getInt("totalPulses"));activeRecipe=pulsesLeft>0?ResourceLocation.tryParse(tag.getString("stewRecipe")):null;
        getTank().setCanFill(!isStewing());getTank().setCanDrain(!isStewing());
    }
}
