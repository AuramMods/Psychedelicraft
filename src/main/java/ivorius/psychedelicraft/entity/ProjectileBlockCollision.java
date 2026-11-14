package ivorius.psychedelicraft.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class ProjectileBlockCollision {
    private static final double EPSILON = 1E-7;

    private ProjectileBlockCollision() {
    }

    @Nullable
    static RayTraceResult trace(Entity projectile, Vec3d start, Vec3d end) {
        Vec3d motion = end.subtract(start);
        double lengthSquared = motion.lengthSquared();
        if (!Double.isFinite(lengthSquared) || lengthSquared < EPSILON) return null;

        Vec3d padding = motion.scale(EPSILON);
        Vec3d from = start.subtract(padding);
        Vec3d sweep = end.add(padding).subtract(from);
        int stepX = (int) Math.signum(sweep.x);
        int stepY = (int) Math.signum(sweep.y);
        int stepZ = (int) Math.signum(sweep.z);
        double strideX = stepX == 0 ? Double.POSITIVE_INFINITY : stepX / sweep.x;
        double strideY = stepY == 0 ? Double.POSITIVE_INFINITY : stepY / sweep.y;
        double strideZ = stepZ == 0 ? Double.POSITIVE_INFINITY : stepZ / sweep.z;
        int x = MathHelper.floor(from.x);
        int y = MathHelper.floor(from.y);
        int z = MathHelper.floor(from.z);
        double nextX = boundaryTime(from.x, x, stepX, strideX);
        double nextY = boundaryTime(from.y, y, stepY, strideY);
        double nextZ = boundaryTime(from.z, z, stepZ, strideZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        AxisAlignedBB segment = new AxisAlignedBB(start.x, start.y, start.z, end.x, end.y, end.z).grow(EPSILON);
        Vec3d probe = start.add(motion.scale(0.001));
        List<AxisAlignedBB> boxes = new ArrayList<>(5);
        World world = projectile.world;

        while (true) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            boxes.clear();
            state.addCollisionBoxToList(world, pos, segment, boxes, projectile, false);
            RayTraceResult hit = clip(boxes, pos, start, end, motion, probe, lengthSquared);
            if (hit != null) return hit;

            double next = Math.min(nextX, Math.min(nextY, nextZ));
            if (next > 1) return null;
            if (next == nextZ) {
                z += stepZ;
                nextZ += strideZ;
            } else if (next == nextY) {
                y += stepY;
                nextY += strideY;
            } else {
                x += stepX;
                nextX += strideX;
            }
        }
    }

    private static double boundaryTime(double start, int cell, int step, double stride) {
        if (step == 0) return Double.POSITIVE_INFINITY;
        return (step > 0 ? cell + 1 - start : start - cell) * stride;
    }

    @Nullable
    private static RayTraceResult clip(List<AxisAlignedBB> boxes, BlockPos pos, Vec3d start, Vec3d end,
                                      Vec3d motion, Vec3d probe, double maxDistance) {
        RayTraceResult closest = null;
        for (AxisAlignedBB box : boxes) {
            if (probe.x >= box.minX && probe.x < box.maxX && probe.y >= box.minY && probe.y < box.maxY
                    && probe.z >= box.minZ && probe.z < box.maxZ) {
                EnumFacing face = EnumFacing.getFacingFromVector((float) motion.x, (float) motion.y, (float) motion.z);
                return new RayTraceResult(probe, face.getOpposite(), pos.toImmutable());
            }
            RayTraceResult hit = entry(box, start, end);
            if (hit == null) continue;
            double distance = start.squareDistanceTo(hit.hitVec);
            if (distance < maxDistance) {
                maxDistance = distance;
                closest = new RayTraceResult(hit.hitVec, hit.sideHit, pos.toImmutable());
            }
        }
        return closest;
    }

    @Nullable
    static RayTraceResult entry(AxisAlignedBB box, Vec3d start, Vec3d end) {
        RayTraceResult hit = box.calculateIntercept(start, end);
        if (hit == null || hit.hitVec.equals(start) || hit.hitVec.equals(end)) return null;
        Vec3d normal = new Vec3d(hit.sideHit.getDirectionVec());
        return end.subtract(start).dotProduct(normal) < 0 ? hit : null;
    }
}
