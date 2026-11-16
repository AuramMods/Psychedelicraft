/* Copyright (c) 2014, Lukas Tenbrink. Native shapes and connections by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class DistilleryBlock extends FluidMachineBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    private static final VoxelShape SHAPE=Shapes.or(Block.box(5,0,5,11,6,11),Block.box(4,0,6,12,5,10),Block.box(6,0,4,10,5,12),Block.box(6,6,6,10,14,10),Block.box(5,9,6,11,13,10),Block.box(6,9,5,10,13,11));
    public DistilleryBlock(){super(Properties.of().strength(1).sound(SoundType.COPPER).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected BlockEntityType<?> machineType(){return PSBlockEntities.DISTILLERY.get();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DistilleryBlockEntity(pos,state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,findOutput(context.getLevel(),context.getClickedPos()));}
    @Override public void neighborChanged(BlockState state,Level world,BlockPos pos,Block block,BlockPos changed,boolean moving){
        if(!world.isClientSide){Direction side=state.getValue(FACING);if(!side.getAxis().isHorizontal()||pos.relative(side).equals(changed)){Direction output=findOutput(world,pos);if(output!=side)world.setBlockAndUpdate(pos,state.setValue(FACING,output));}}
    }
    private static Direction findOutput(Level world,BlockPos pos){for(Direction side:Direction.Plane.HORIZONTAL)if(canConnectTo(world,pos,side))return side;return Direction.UP;}
    public static boolean canConnectTo(Level world,BlockPos pos,Direction side){
        BlockPos destination=pos.relative(side);if(!world.hasChunkAt(destination))return false;BlockState state=world.getBlockState(destination);
        return state.getBlock() instanceof DistilleryBlock?state.getValue(FACING)!=side.getOpposite():state.getBlock() instanceof FluidMachineBlock;
    }
}
