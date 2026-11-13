package ivorius.psychedelicraft.block.entity;

public final class FlaskBlockEntity extends FluidMachineBlockEntity {
    public static final int CAPACITY = 8000;

    public FlaskBlockEntity() {
        super(CAPACITY);
    }

    @Override
    protected void tickProcessing() {
        // The flask stores and transfers fluid through the shared vessel slots.
    }
}
