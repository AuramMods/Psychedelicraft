/* Copyright (c) 2014, Lukas Tenbrink. Native shapes and orientation by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class BarrelBlock extends FluidMachineBlock {
    public static final DirectionProperty FACING=HopperBlock.FACING;
    public static final BooleanProperty TAPPED=BooleanProperty.create("tapped");
    private static final VoxelShape X=Shapes.or(Block.box(0,5,2,16,13,14),Block.box(0,3,4,16,15,12)),Y=Shapes.or(Block.box(2,0,4,14,16,12),Block.box(4,0,2,12,16,14)),Z=Shapes.or(Block.box(2,5,0,14,13,16),Block.box(4,3,0,12,15,16));
    public BarrelBlock(Block timber){super(Properties.of().mapColor(timber.defaultMapColor()).strength(2).sound(SoundType.WOOD).ignitedByLava());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(TAPPED,true));}
    @Override protected BlockEntityType<?> machineType(){return PSBlockEntities.BARREL.get();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BarrelBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,TAPPED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){Direction side=context.getNearestLookingDirection().getOpposite();return defaultBlockState().setValue(FACING,side.getAxis().isVertical()?Direction.DOWN:side);}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING).getAxis()){case X->X;case Y->Y;case Z->Z;};}
    @Override public int getFlammability(BlockState state,BlockGetter world,BlockPos pos,Direction face){return 20;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter world,BlockPos pos,Direction face){return 5;}
}
