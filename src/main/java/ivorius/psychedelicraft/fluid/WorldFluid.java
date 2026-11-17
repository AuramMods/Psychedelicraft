/* Copyright (c) 2014, Lukas Tenbrink. Native flow adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.fluid;
import ivorius.psychedelicraft.block.PhysicalFluidBlock;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;
import net.minecraftforge.fluids.ForgeFlowingFluid;
/** Keep native flow, slopes and levels while carrying arbitrary accepted batch attributes. */
public abstract class WorldFluid extends ForgeFlowingFluid {
    public final PSFluids.Family family;
    private WorldFluid(PSFluids.Family family){super(family.properties());this.family=family;registerDefaultState(defaultFluidState().setValue(FALLING,false));}
    @Override protected void spread(Level level,BlockPos pos,FluidState state){
        if(!state.isEmpty()&&!state.isSource()&&isSame(state.getType()))family.block.get().setFluid(level,pos,state,PhysicalFluidBlock.neighborAttributes(level,pos,this));
        super.spread(level,pos,state);
    }
    @Override protected void spreadTo(LevelAccessor level,BlockPos pos,BlockState target,Direction direction,FluidState state){
        var attributes=PhysicalFluidBlock.neighborAttributes(level,pos,this);
        super.spreadTo(level,pos,target,direction,state);
        if(level.getBlockState(pos).is(family.block.get()))family.block.get().setFluid(level,pos,state,attributes);
    }
    @Override protected boolean canBeReplacedWith(FluidState state,BlockGetter level,BlockPos pos,Fluid incoming,Direction direction){return family.waterLike()&&incoming.is(net.minecraft.tags.FluidTags.LAVA)&&direction==Direction.DOWN;}
    @Override protected boolean isRandomlyTicking(){return family==PSFluids.COFFEE;}
    @Override protected void randomTick(Level level,BlockPos pos,FluidState state,RandomSource random){
        var contents=family.block.get().contents(level,pos);int warmth=PSFluids.warmth(contents);if(family!=PSFluids.COFFEE||warmth==0)return;
        var attributes=contents.getOrCreateTag();if(warmth==1)attributes.remove("warmth");else attributes.putInt("warmth",warmth-1);
        family.block.get().setFluid(level,pos,state,attributes);
    }
    @Override public net.minecraft.core.particles.ParticleOptions getDripParticle(){return net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER;}
    @Override public void animateTick(Level level,BlockPos pos,FluidState state,RandomSource random){
        var water=state.isSource()?Fluids.WATER.getSource(state.getValue(FALLING)):Fluids.FLOWING_WATER.getFlowing(state.getAmount(),state.getValue(FALLING));
        water.animateTick(level,pos,random);
        if(family==PSFluids.SLURRY){
            for(int i=0;i<5;i++)level.addParticle(new ivorius.psychedelicraft.particle.BubbleParticleOptions(PSFluids.color(family.block.get().contents(level,pos)),1),pos.getX()+.5+random.triangle(0,.5),pos.getY()+state.getHeight(level,pos),pos.getZ()+.5+random.triangle(0,.5),0,0,0);
            level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,net.minecraft.sounds.SoundSource.BLOCKS,.5F+random.nextFloat(),.3F+random.nextFloat(),true);
        }
    }
    public static final class Source extends WorldFluid {
        public Source(PSFluids.Family family){super(family);}
        @Override public boolean isSource(FluidState state){return true;}
        @Override public int getAmount(FluidState state){return 8;}
    }
    public static final class Flowing extends WorldFluid {
        public Flowing(PSFluids.Family family){super(family);registerDefaultState(defaultFluidState().setValue(LEVEL,7));}
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> builder){super.createFluidStateDefinition(builder);builder.add(LEVEL);}
        @Override public boolean isSource(FluidState state){return false;}
        @Override public int getAmount(FluidState state){return state.getValue(LEVEL);}
    }
}
