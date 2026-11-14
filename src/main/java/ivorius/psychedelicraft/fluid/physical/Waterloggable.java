package ivorius.psychedelicraft.fluid.physical;

public interface Waterloggable {
    default boolean alwaysWaterlogged() {
        return false;
    }

    default boolean hasOwnWaterTile() {
        return false;
    }

    default boolean canPistonMove() {
        return true;
    }
}
