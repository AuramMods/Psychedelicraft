/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block.entity;
import ivorius.psychedelicraft.block.DistilleryBlock;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.Locale;

public final class DistilleryBlockEntity extends FluidProcessingBlockEntity {
    private Outlet outlet=Outlet.MISSING;
    public DistilleryBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.DISTILLERY.get(),pos,state,FlaskBlockEntity.CAPACITY);}
    public Outlet getOutlet(){return outlet;}
    @Override protected void tickProcessing(){
        FluidStack contents=getTank().getFluid();BrewProcess process=BrewProcess.findDistillation(contents);ProcessingTank output=findOutput();
        FluidStack byproduct=process==BrewProcess.DISTILL?BrewProcess.distillationOutput(contents):FluidStack.EMPTY;
        Outlet status=output==null?Outlet.MISSING:byproduct.isEmpty()||output.fillExactly(byproduct,FluidAction.SIMULATE)?Outlet.READY:Outlet.BLOCKED;
        if(status!=outlet){outlet=status;changed();}
        if(!contents.isEmpty()&&(PSFluids.brew(contents.getFluid())!=null||PSFluids.isFlowerExtract(contents.getFluid())||contents.getFluid()==PSFluids.COFFEE.get()||contents.getFluid()==PSFluids.SLURRY.get())){
            if(level.random.nextInt(120)==0)steam();if(level.random.nextInt(50)==0)bubble();
        }
        int duration=process==BrewProcess.IDLE?0:distillationTicks(process.duration(contents),heatRate(level.getBlockState(worldPosition.below())));
        if(outlet==Outlet.MISSING)clearProcessingProgress();processFluid(process,duration,outlet==Outlet.READY);
    }
    public static int distillationTicks(int base,int rate){return 1+Math.max(1,Math.min(Integer.MAX_VALUE-1,base/rate));}
    public static int heatRate(BlockState below){
        if(below.getFluidState().is(FluidTags.LAVA))return 7;
        if(below.is(BlockTags.FIRE)||below.is(BlockTags.CAMPFIRES))return 3;
        return below.is(Blocks.MAGMA_BLOCK)?2:1;
    }
    @Override protected boolean completeProcess(BrewProcess process,FluidStack contents){
        ProcessingTank output=findOutput();if(output==null)return false;
        FluidStack byproduct=BrewProcess.distillationOutput(contents);
        if(!byproduct.isEmpty()&&!output.fillExactly(byproduct,FluidAction.EXECUTE))return false;
        steam();if(level.random.nextInt(10)==0)bubble();return super.completeProcess(process,contents);
    }
    private ProcessingTank findOutput(){
        Direction side=getBlockState().getValue(DistilleryBlock.FACING);
        if(!side.getAxis().isHorizontal()||!DistilleryBlock.canConnectTo(level,worldPosition,side))return null;
        return level.getBlockEntity(worldPosition.relative(side)) instanceof FluidMachineBlockEntity machine&&!machine.isRemoved()?machine.getTank():null;
    }
    private void steam(){((ServerLevel)level).sendParticles(ParticleTypes.CLOUD,worldPosition.getX()+(level.random.nextDouble()+level.random.nextDouble())*.5,worldPosition.getY()+.6,worldPosition.getZ()+(level.random.nextDouble()+level.random.nextDouble())*.5,2,0,0,0,0);}
    private void bubble(){level.playSound(null,worldPosition,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,.25F,.5F);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("outlet",outlet.ordinal());}
    @Override public void load(CompoundTag tag){super.load(tag);int id=tag.getInt("outlet");outlet=id>=0&&id<Outlet.values().length?Outlet.values()[id]:Outlet.MISSING;}
    public enum Outlet{MISSING,READY,BLOCKED;public String key(){return "psychedelicraft.distillery.outlet."+name().toLowerCase(Locale.ROOT);}}
}
