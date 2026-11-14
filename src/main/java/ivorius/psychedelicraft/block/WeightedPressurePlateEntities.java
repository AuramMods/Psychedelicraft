package ivorius.psychedelicraft.block;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public final class WeightedPressurePlateEntities {
    private WeightedPressurePlateEntities() {}

    public static <T extends Entity> List<T> find(World world, Class<? extends T> type, AxisAlignedBB bounds) {
        return world.getEntitiesWithinAABB(type, bounds,
                entity -> EntitySelectors.NOT_SPECTATING.apply(entity) && !entity.doesEntityNotTriggerPressurePlate());
    }
}
