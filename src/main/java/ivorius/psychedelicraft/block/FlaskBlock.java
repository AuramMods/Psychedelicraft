/* Copyright (c) 2014, Lukas Tenbrink. Native shapes by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class FlaskBlock extends FluidMachineBlock {
    private static final VoxelShape SHAPE=Shapes.or(Block.box(5,0,5,11,5,11),Block.box(4,0,6,12,4,10),Block.box(6,0,4,10,4,12),Block.box(6,5,6,10,11,10),Block.box(5,7,6,11,10,10),Block.box(6,7,5,10,10,11));
    public FlaskBlock(){super(Properties.of().strength(1).sound(SoundType.COPPER).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));}
    @Override protected BlockEntityType<?> machineType(){return PSBlockEntities.FLASK.get();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FlaskBlockEntity(pos,state);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return SHAPE;}
}
