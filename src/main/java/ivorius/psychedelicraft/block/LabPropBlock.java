/* Copyright (c) 2014, Lukas Tenbrink. Native shapes by Sollace. */
package ivorius.psychedelicraft.block;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public class LabPropBlock extends Block {
    private static final VoxelShape BURNER=Block.box(3,0,3,13,2,13);
    protected LabPropBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(.7F).sound(SoundType.METAL).noOcclusion().noLootTable());}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return BURNER;}
    public static final class Tray extends LabPropBlock {
        public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape X=Block.box(-1,0,2,17,2,14),Z=Block.box(2,0,-1,14,2,17);
        public Tray(){registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.X));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AXIS);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(AXIS,context.getHorizontalDirection().getClockWise().getAxis());}
        @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return state.getValue(AXIS)==Direction.Axis.X?X:Z;}
        @Override public BlockState rotate(BlockState state,Rotation rotation){return rotation==Rotation.CLOCKWISE_90||rotation==Rotation.COUNTERCLOCKWISE_90?state.cycle(AXIS):state;}
    }
}
