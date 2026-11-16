/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
public final class FlaskBlockEntity extends FluidMachineBlockEntity {
    public static final int CAPACITY=8000;
    public FlaskBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.FLASK.get(),pos,state,CAPACITY);}
    @Override protected void tickProcessing(){}
}
