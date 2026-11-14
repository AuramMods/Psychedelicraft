package ivorius.psychedelicraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public final class ProjectileGateway {
    private ProjectileGateway() {
    }

    public static <T extends Entity> List<T> find(World world, Class<? extends T> type, AxisAlignedBB area) {
        return world.getEntitiesWithinAABB(type, area,
                entity -> EntitySelectors.NOT_SPECTATING.apply(entity)
                        && (!(entity instanceof MolotovCocktailEntity) || entity.timeUntilPortal <= 0));
    }

    public static void teleport(Entity entity, double x, double y, double z) {
        entity.setPositionAndUpdate(x, y, z);
        if (entity instanceof MolotovCocktailEntity) entity.timeUntilPortal = entity.getPortalCooldown();
    }
}
