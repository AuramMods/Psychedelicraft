package ivorius.psychedelicraft.fluid.physical;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class PhysicalFluidCurrents {
    private static final double WATER_PUSH = 0.014D;
    private static final double MIN_PUSH = 0.0045D;

    private PhysicalFluidCurrents() {}

    public static Vec3d accumulateLegacy(Block block, World world, BlockPos pos, Entity entity, Vec3d motion) {
        return block instanceof PhysicalFluidBlock ? motion : block.modifyAcceleration(world, pos, entity, motion);
    }

    public static boolean apply(boolean legacyContact, World world, Material material, Entity entity) {
        if (material != Material.WATER) return legacyContact;
        AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.001D);
        BlockPos min = new BlockPos(box.minX, box.minY, box.minZ);
        BlockPos max = new BlockPos(MathHelper.ceil(box.maxX) - 1, MathHelper.ceil(box.maxY) - 1,
                MathHelper.ceil(box.maxZ) - 1);
        if (!world.isAreaLoaded(min, max)) return legacyContact;

        boolean contact = false;
        boolean pushed = entity.isPushedByWater();
        double depth = 0D;
        Vec3d flow = Vec3d.ZERO;
        int samples = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    pos.setPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    if (!(state.getBlock() instanceof PhysicalFluidBlock)) continue;
                    PhysicalFluidBlock fluid = (PhysicalFluidBlock) state.getBlock();
                    if (!fluid.isWaterLike()) continue;
                    double surface = y + (double) fluid.surfaceHeight(world, pos, state);
                    if (surface < box.minY) continue;
                    contact = true;
                    depth = Math.max(depth, surface - box.minY);
                    if (pushed) {
                        flow = flow.add(fluid.getFlowVector(world, pos).scale(depth < 0.4D ? depth : 1D));
                        samples++;
                    }
                }
            }
        }
        if (samples > 0 && flow.lengthSquared() > 0) {
            Vec3d push = flow.scale(1D / samples);
            if (!(entity instanceof EntityPlayer)) push = push.normalize();
            push = push.scale(WATER_PUSH);
            if (Math.abs(entity.motionX) < 0.003D && Math.abs(entity.motionZ) < 0.003D
                    && push.lengthSquared() < MIN_PUSH * MIN_PUSH) {
                push = push.normalize().scale(MIN_PUSH);
            }
            entity.motionX += push.x;
            entity.motionY += push.y;
            entity.motionZ += push.z;
        }
        return legacyContact || contact;
    }
}
