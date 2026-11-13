package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class TrayRayTrace {
    private static final double OVERHANG = 1D / 16;
    private static final double HEIGHT = 2D / 16;
    private static final double EPSILON = 1E-7;

    private TrayRayTrace() {
    }

    @Nullable
    public static RayTraceResult visitCell(World world, BlockPos cell, Vec3d start, Vec3d end,
                                           @Nullable RayTraceResult trayHit) {
        // Only consider neighbors if the ray reaches the height and outer strip of a tray lip.
        double y = cell.getY();
        if (Math.max(start.y, end.y) < y || Math.min(start.y, end.y) > y + HEIGHT) return trayHit;
        double dy = end.y - start.y;
        double from = 0;
        double to = 1;
        if (dy != 0) {
            double bottom = (y - start.y) / dy;
            double top = (y + HEIGHT - start.y) / dy;
            from = Math.max(0, Math.min(bottom, top));
            to = Math.min(1, Math.max(bottom, top));
        }

        double x1 = start.x + (end.x - start.x) * from - cell.getX();
        double x2 = start.x + (end.x - start.x) * to - cell.getX();
        double z1 = start.z + (end.z - start.z) * from - cell.getZ();
        double z2 = start.z + (end.z - start.z) * to - cell.getZ();

        if (Math.min(x1, x2) <= OVERHANG + EPSILON && Math.max(x1, x2) >= -EPSILON) {
            trayHit = hitFrom(world, cell, start, end, EnumFacing.WEST, trayHit);
        }
        if (Math.max(x1, x2) >= 1 - OVERHANG - EPSILON && Math.min(x1, x2) <= 1 + EPSILON) {
            trayHit = hitFrom(world, cell, start, end, EnumFacing.EAST, trayHit);
        }
        if (Math.min(z1, z2) <= OVERHANG + EPSILON && Math.max(z1, z2) >= -EPSILON) {
            trayHit = hitFrom(world, cell, start, end, EnumFacing.NORTH, trayHit);
        }
        if (Math.max(z1, z2) >= 1 - OVERHANG - EPSILON && Math.min(z1, z2) <= 1 + EPSILON) {
            trayHit = hitFrom(world, cell, start, end, EnumFacing.SOUTH, trayHit);
        }
        return trayHit;
    }

    @Nullable
    private static RayTraceResult hitFrom(World world, BlockPos cell, Vec3d start, Vec3d end,
                                         EnumFacing direction, @Nullable RayTraceResult previous) {
        BlockPos owner = cell.offset(direction);
        if (!world.isBlockLoaded(owner)) return previous;
        IBlockState state = world.getBlockState(owner);
        if (!(state.getBlock() instanceof LabPropBlock.Tray) || state.getValue(LabPropBlock.Tray.AXIS) != direction.getAxis()) {
            return previous;
        }

        RayTraceResult hit = state.collisionRayTrace(world, owner, start, end);
        if (hit == null || !inCell(hit.hitVec, cell)) return previous;
        return nearest(previous, end, hit);
    }

    private static boolean inCell(Vec3d point, BlockPos cell) {
        double x = point.x - cell.getX();
        double y = point.y - cell.getY();
        double z = point.z - cell.getZ();
        return x >= -EPSILON && x <= 1 + EPSILON && y >= -EPSILON && y <= 1 + EPSILON
                && z >= -EPSILON && z <= 1 + EPSILON;
    }

    @Nullable
    public static RayTraceResult nearest(@Nullable RayTraceResult vanilla, Vec3d end, @Nullable RayTraceResult trayHit) {
        // Both hits lie on the original segment, so the earlier hit is farther from its endpoint.
        if (trayHit != null && (vanilla == null || vanilla.typeOfHit == RayTraceResult.Type.MISS
                || end.squareDistanceTo(trayHit.hitVec) > end.squareDistanceTo(vanilla.hitVec))) return trayHit;
        return vanilla;
    }
}
