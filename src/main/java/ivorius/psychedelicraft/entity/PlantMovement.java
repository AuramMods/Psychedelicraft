package ivorius.psychedelicraft.entity;

/** Access to the next-move slowdown on the transformed 1.12 entity. */
public interface PlantMovement {
    void psychedelicraft$slowNextMove();

    static double horizontalMultiplier(boolean plant) {
        return plant ? 0.8F : 0.25D;
    }

    static double verticalMultiplier(boolean plant) {
        return plant ? 0.75D : (double) 0.05F;
    }
}
