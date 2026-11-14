package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.block.BubbleColumnBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

import javax.annotation.Nullable;

public final class PhysicalFluidCurrents {
    private static final double WATER_PUSH = 0.014D;
    private static final double MIN_PUSH = 0.0045D;

    private PhysicalFluidCurrents() {}

    public static Vec3d accumulateLegacy(Block block, World world, BlockPos pos, Entity entity, Vec3d motion,
                                         @Nullable Vec3d mixedPush) {
        if (block instanceof PhysicalFluidBlock) return motion;
        if (mixedPush != null) {
            IBlockState state = world.getBlockState(pos);
            if (isNativeWater(state) || Waterlogging.isWet(world, pos, state)) return motion;
        }
        return block.modifyAcceleration(world, pos, entity, motion);
    }

    @Nullable
    public static Vec3d mixedPush(World world, Material material, Entity entity) {
        if (material != Material.WATER) return null;
        AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.001D);
        BlockPos min = new BlockPos(box.minX, box.minY, box.minZ);
        BlockPos max = new BlockPos(MathHelper.ceil(box.maxX) - 1, MathHelper.ceil(box.maxY) - 1,
                MathHelper.ceil(box.maxZ) - 1);
        if (!world.isAreaLoaded(min, max) || !hasPhysicalContact(world, box.minY, min, max)) return null;
        if (!entity.isPushedByWater()) return Vec3d.ZERO;

        double depth = 0D;
        Vec3d flow = Vec3d.ZERO;
        int samples = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    pos.setPos(x, y, z);
                    IBlockState fluid = Waterlogging.fluidState(world, pos);
                    float height = waterHeight(world, pos, fluid);
                    if (height == 0F) continue;
                    double surface = y + (double) height;
                    if (surface < box.minY) continue;
                    depth = Math.max(depth, surface - box.minY);
                    flow = flow.add(flowVector(world, pos, fluid).scale(depth < 0.4D ? depth : 1D));
                    samples++;
                }
            }
        }
        if (samples == 0 || flow.lengthSquared() == 0) return Vec3d.ZERO;
        Vec3d push = flow.scale(1D / samples);
        if (!(entity instanceof EntityPlayer)) push = push.normalize();
        push = push.scale(WATER_PUSH);
        if (Math.abs(entity.motionX) < 0.003D && Math.abs(entity.motionZ) < 0.003D
                && push.lengthSquared() < MIN_PUSH * MIN_PUSH) {
            push = push.normalize().scale(MIN_PUSH);
        }
        return push;
    }

    public static boolean apply(boolean legacyContact, Entity entity, @Nullable Vec3d push) {
        if (push == null) return legacyContact;
        entity.motionX += push.x;
        entity.motionY += push.y;
        entity.motionZ += push.z;
        return true;
    }

    private static boolean hasPhysicalContact(World world, double minY, BlockPos min, BlockPos max) {
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) return false;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            IBlockState fluid = world.getBlockState(pos);
            if (fluid.getBlock() instanceof PhysicalFluidBlock) {
                PhysicalFluidBlock physical = (PhysicalFluidBlock) fluid.getBlock();
                if (physical.isWaterLike() && pos.getY() + (double) physical.surfaceHeight(world, pos, fluid) >= minY) return true;
            }
        }
        return false;
    }

    private static float waterHeight(IBlockAccess world, BlockPos pos, IBlockState state) {
        if (state.getBlock() instanceof PhysicalFluidBlock) {
            PhysicalFluidBlock physical = (PhysicalFluidBlock) state.getBlock();
            return physical.isWaterLike() ? physical.surfaceHeight(world, pos, state) : 0F;
        }
        if (!isNativeWater(state)) return 0F;
        return isNativeWater(Waterlogging.fluidState(world, pos.up())) ? 1F : flowHeight(world, pos, state);
    }

    static Vec3d flowVector(IBlockAccess world, BlockPos pos, IBlockState state) {
        Block fluid = state.getBlock();
        float height = flowHeight(world, pos, state);
        Vec3d flow = Vec3d.ZERO;
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.offset(side);
            IBlockState adjacent = Waterlogging.fluidState(world, neighbor);
            float difference = 0F;
            if (matchesFluid(fluid, adjacent)) {
                difference = height - flowHeight(world, neighbor, adjacent);
            } else if (!adjacent.getMaterial().blocksMovement() && !adjacent.getMaterial().isLiquid()
                    && !(adjacent.getBlock() instanceof IFluidBlock)) {
                BlockPos below = neighbor.down();
                IBlockState lower = Waterlogging.fluidState(world, below);
                if (matchesFluid(fluid, lower)) difference = height - (flowHeight(world, below, lower) - 8F / 9F);
            }
            flow = flow.addVector(side.getFrontOffsetX() * difference, 0, side.getFrontOffsetZ() * difference);
        }

        boolean falling = fluid instanceof PhysicalFluidBlock ? ((PhysicalFluidBlock) fluid).isFalling(world, pos, state)
                : state.getValue(BlockLiquid.LEVEL) >= 8;
        if (falling) {
            for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.offset(side);
                if (hasCurrentWall(fluid, world, neighbor, side) || hasCurrentWall(fluid, world, neighbor.up(), side)) {
                    return flow.normalize().addVector(0, -6, 0).normalize();
                }
            }
        }
        return flow.normalize();
    }

    private static float flowHeight(IBlockAccess world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof PhysicalFluidBlock) return ((PhysicalFluidBlock) block).flowHeight(world, pos, state);
        int level = state.getValue(BlockLiquid.LEVEL);
        return (level >= 8 ? 8 : 8 - level) / 9F;
    }

    private static boolean isNativeWater(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.WATER || block == Blocks.FLOWING_WATER || BubbleColumnBlock.isColumn(state);
    }

    private static boolean matchesFluid(Block fluid, IBlockState state) {
        return fluid instanceof PhysicalFluidBlock ? state.getBlock() == fluid : isNativeWater(state);
    }

    private static boolean hasCurrentWall(Block fluid, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (matchesFluid(fluid, Waterlogging.fluidState(world, pos))) return false;
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial() != Material.ICE && state.getBlockFaceShape(world, pos, side) == BlockFaceShape.SOLID;
    }
}
