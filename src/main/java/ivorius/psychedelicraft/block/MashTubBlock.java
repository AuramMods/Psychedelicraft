/* Copyright (c) 2014, Lukas Tenbrink. Native basin design by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.network.NetworkHooks;

public final class MashTubBlock extends FluidMachineBlock implements LiquidBlockContainer {
    public static final int CENTER=4;
    public static final IntegerProperty OFFSET=IntegerProperty.create("offset",0,8),LIGHT=IntegerProperty.create("light",0,15);
    private static final VoxelShape BASIN=Shapes.or(Block.box(-8,0,-8,24,1,24),Block.box(-8,0,-8,24,16,-7),Block.box(-8,0,23,24,16,24),Block.box(-8,0,-7,-7,16,23),Block.box(23,0,-7,24,16,23));
    public MashTubBlock(){super(Properties.of().strength(2).sound(SoundType.WOOD).ignitedByLava().pushReaction(PushReaction.BLOCK).lightLevel(state->state.getValue(LIGHT)));registerDefaultState(stateDefinition.any().setValue(OFFSET,CENTER).setValue(LIGHT,0));}
    @Override protected BlockEntityType<?> machineType(){return PSBlockEntities.MASH_TUB.get();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MashTubBlockEntity(pos,state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(OFFSET,LIGHT);}
    public static BlockPos center(BlockPos pos,BlockState state){int offset=state.getValue(OFFSET);return pos.offset(1-offset/3,0,1-offset%3);}
    public static BlockState wallState(int dx,int dz){return PSBlocks.MASH_TUB_EDGE.get().defaultBlockState().setValue(OFFSET,(dx+1)*3+dz+1);}
    public static VoxelShape collision(BlockState state){int offset=state.getValue(OFFSET);return Shapes.join(BASIN.move(1-offset/3,0,1-offset%3),Shapes.block(),BooleanOp.AND);}
    public static VoxelShape outline(BlockState state){int offset=state.getValue(OFFSET),dx=1-offset/3,dz=1-offset%3;return Shapes.box(Math.max(0,dx-.5),0,Math.max(0,dz-.5),Math.min(1,dx+1.5),1,Math.min(1,dz+1.5));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return outline(state);}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return collision(state);}
    public static MashTubBlockEntity getVat(BlockGetter world,BlockPos pos){
        if(world==null||world instanceof Level level&&!level.hasChunkAt(pos))return null;BlockState state=world.getBlockState(pos);
        if(!(state.getBlock() instanceof MashTubBlock)&&!(state.getBlock() instanceof MashTubWallBlock))return null;
        BlockPos owner=center(pos,state);if(world instanceof Level level&&!level.hasChunkAt(owner))return null;
        return world.getBlockState(owner).getBlock() instanceof MashTubBlock&&world.getBlockEntity(owner) instanceof MashTubBlockEntity vat&&!vat.isRemoved()&&!vat.isDismantling()?vat:null;
    }
    public static InteractionResult interact(Level world,BlockPos pos,Player player,InteractionHand hand){
        MashTubBlockEntity vat=getVat(world,pos);if(vat==null)return InteractionResult.PASS;
        if(!world.isClientSide&&!vat.interactWithVessel(player,hand)){
            var held=player.getItemInHand(hand);if(vat.addIngredient(held)){if(!player.getAbilities().instabuild)held.shrink(1);}
            else if(!vat.dropSolidOutput(player)&&player instanceof ServerPlayer server)NetworkHooks.openScreen(server,vat,vat.getBlockPos());
        }return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(world,pos,player,hand);}
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&!world.isClientSide&&world.getBlockEntity(pos) instanceof MashTubBlockEntity vat){
            vat.beginDismantling();for(var stack:vat.takeSuppliedIngredients())Block.popResource(world,pos,stack);
            for(BlockPos part:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,0,1))){BlockState other=world.getBlockState(part);if(other.getBlock() instanceof MashTubWallBlock&&center(part,other).equals(pos))world.removeBlock(part,false);}
        }super.onRemove(state,world,pos,replacement,moving);
    }
    public static boolean canAcceptLiquid(BlockGetter world,BlockPos pos,Fluid fluid){var vat=getVat(world,pos);return vat!=null&&(fluid==Fluids.WATER||fluid==Fluids.LAVA||fluid instanceof ivorius.psychedelicraft.fluid.WorldFluid)&&vat.getTank().fillExactly(incomingFluid(world,pos,fluid),FluidAction.SIMULATE);}
    public static boolean acceptLiquid(LevelAccessor world,BlockPos pos,FluidState fluid){
        if(!canAcceptLiquid(world,pos,fluid.getType()))return false;if(world.isClientSide())return true;return getVat(world,pos).getTank().fillExactly(incomingFluid(world,pos,fluid.getType()),FluidAction.EXECUTE);
    }
    private static FluidStack incomingFluid(BlockGetter world,BlockPos pos,Fluid fluid){return new FluidStack(ivorius.psychedelicraft.fluid.PSFluids.canonical(fluid),1000,fluid instanceof ivorius.psychedelicraft.fluid.WorldFluid?PhysicalFluidBlock.neighborAttributes(world,pos,fluid):null);}
    @Override public boolean canPlaceLiquid(BlockGetter world,BlockPos pos,BlockState state,Fluid fluid){return canAcceptLiquid(world,pos,fluid);}
    @Override public boolean placeLiquid(LevelAccessor world,BlockPos pos,BlockState state,FluidState fluid){return acceptLiquid(world,pos,fluid);}
}
