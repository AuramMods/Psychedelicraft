package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.CampfireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;

public final class CampfireProjectile {
    private static final String FIREWORK_OWNER = "psychedelicraft:fireworkOwner";

    private CampfireProjectile() {
    }

    public static RayTraceResult.Type checkBobberHit(Entity bobber, RayTraceResult hit) {
        if (isCampfireHit(bobber, hit) && notifyImpact(bobber, hit)) return RayTraceResult.Type.MISS;
        return hit.typeOfHit;
    }

    public static void setFireworkOwner(Entity rocket, EntityPlayer player) {
        rocket.getEntityData().setUniqueId(FIREWORK_OWNER, player.getUniqueID());
    }

    public static Entity fireworkOwner(Entity rocket) {
        if (!(rocket.world instanceof WorldServer) || !rocket.getEntityData().hasUniqueId(FIREWORK_OWNER)) return null;
        return ((WorldServer) rocket.world).getEntityFromUuid(rocket.getEntityData().getUniqueId(FIREWORK_OWNER));
    }

    public static void moveFirework(Entity rocket, MoverType type, double x, double y, double z) {
        rocket.move(type, x, y, z);
        // The donor traces after movement, using the velocity from before collision resolution.
        checkFireworkHit(rocket, x, y, z);
    }

    public static void checkAttachedFireworkHit(Entity rocket) {
        if (((EntityFireworkRocket) rocket).isAttachedToEntity()) {
            checkFireworkHit(rocket, rocket.motionX, rocket.motionY, rocket.motionZ);
        }
    }

    private static void checkFireworkHit(Entity rocket, double x, double y, double z) {
        if (rocket.world.isRemote || rocket.noClip || !rocket.isBurning()) return;
        Vec3d start = rocket.getPositionVector();
        RayTraceResult hit = ProjectileBlockCollision.trace(rocket, start, start.addVector(x, y, z));
        if (!isCampfireHit(rocket, hit)) return;
        Entity owner = fireworkOwner(rocket);
        AxisAlignedBB search = rocket.getEntityBoundingBox().expand(x, y, z).grow(1D);
        for (Entity entity : rocket.world.getEntitiesInAABBexcluding(rocket, search,
                candidate -> candidate != owner && candidate.isEntityAlive() && candidate.canBeCollidedWith())) {
            if (ProjectileBlockCollision.entry(entity.getEntityBoundingBox().grow(0.3D), start, hit.hitVec) != null) return;
        }
        notifyImpact(rocket, hit);
    }

    private static boolean isCampfireHit(Entity projectile, RayTraceResult hit) {
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK
                && projectile.world.getBlockState(hit.getBlockPos()).getBlock() instanceof CampfireBlock;
    }

    private static boolean notifyImpact(Entity projectile, RayTraceResult hit) {
        return CampfireBlock.afterProjectileImpact(projectile, hit, ForgeEventFactory.onProjectileImpact(projectile, hit));
    }
}
