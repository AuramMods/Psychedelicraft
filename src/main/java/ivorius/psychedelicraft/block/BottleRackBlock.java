/* Copyright (c) 2014, Lukas Tenbrink. Native rack design by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.util.*;
public final class BottleRackBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction,VoxelShape> SHAPES=new EnumMap<>(Direction.class);
    static{
        VoxelShape shape=Shapes.or(Block.box(0,0,5,1,16,13),Block.box(5,0,5,6,16,13),Block.box(10,0,5,11,16,13),Block.box(15,0,5,16,16,13),Block.box(1,0,3.7,15,.9,13.2),Block.box(1,5,3.7,15,6,13.2),Block.box(1,10,3.7,15,11,13.2),Block.box(1,15.1,3.7,15,16,13.2));
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){SHAPES.put(facing,shape);VoxelShape[] rotated={Shapes.empty()};shape.forAllBoxes((x0,y0,z0,x1,y1,z1)->rotated[0]=Shapes.or(rotated[0],Shapes.box(1-z1,y0,x0,1-z0,y1,x1)));shape=rotated[0];}
    }
    public BottleRackBlock(){super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public int getFlammability(BlockState state,BlockGetter world,BlockPos pos,Direction side){return 20;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter world,BlockPos pos,Direction side){return 5;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BottleRackBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return SHAPES.get(state.getValue(FACING));}
    public static int slotAt(Direction facing,Direction side,double x,double y,double z){
        if(!facing.getAxis().isHorizontal()||side!=facing&&side.getAxis().isHorizontal())return -1;double horizontal=switch(facing){case NORTH->1-x;case SOUTH->x;case WEST->z;case EAST->1-z;default->-1;};
        return Mth.clamp(Mth.floor(horizontal*3),0,2)+3*Mth.clamp(Mth.floor((1-y)*3),0,2);
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        Vec3 location=hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));int slot=slotAt(state.getValue(FACING),hit.getDirection(),location.x,location.y,location.z);
        if(slot<0||!world.mayInteract(player,pos)||!(world.getBlockEntity(pos) instanceof BottleRackBlockEntity rack))return InteractionResult.PASS;
        var handler=rack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);ItemStack held=player.getItemInHand(hand);boolean removing=held.isEmpty();
        if(removing){ItemStack taken=handler.extractItem(slot,1,world.isClientSide);if(!world.isClientSide&&!taken.isEmpty())player.setItemInHand(hand,taken);}
        else{ItemStack remainder=handler.insertItem(slot,held,true);if(remainder.getCount()==held.getCount())return InteractionResult.PASS;if(!world.isClientSide){remainder=handler.insertItem(slot,held,false);if(!player.getAbilities().instabuild)player.setItemInHand(hand,remainder);}}
        if(!world.isClientSide)world.playSound(null,pos,PSSounds.BOTTLE_RACK_USE.get(),SoundSource.BLOCKS,1,removing?1:1.5F);return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState replacement,boolean moving){if(!state.is(replacement.getBlock())&&!world.isClientSide&&world.getBlockEntity(pos) instanceof BottleRackBlockEntity rack){rack.invalidateCaps();Containers.dropContents(world,pos,rack);world.updateNeighbourForOutputSignal(pos,this);}super.onRemove(state,world,pos,replacement,moving);}
}
