package ivorius.psychedelicraft.fluid.physical;

public interface Waterloggable {
    default boolean hasOwnWaterTile() {
        return false;
    }

    default boolean canPistonMove() {
        return true;
    }
}
