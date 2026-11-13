package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.IFluidBlock;

import javax.annotation.Nullable;

public final class BoatFlotation {
    private static final double CONTACT_DEPTH = 0.001;

    private BoatFlotation() {
    }

    public static float waterLevelAbove(EntityBoat boat, double lastVerticalMotion) {
        AxisAlignedBB hull = boat.getEntityBoundingBox();
        int firstY = MathHelper.floor(hull.maxY);
        int endY = MathHelper.ceil(hull.maxY - lastVerticalMotion);
        for (int y = firstY; y < endY; y++) {
            float height = 0;
            for (BlockPos pos : cells(hull, y, y + 1)) {
                IBlockState fluid = Waterlogging.fluidState(boat.world, pos);
                height = Math.max(height, waterHeight(boat.world, pos, fluid, new AxisAlignedBB(pos)));
                if (height >= 1) break;
            }
            if (height < 1) return y + height;
        }
        return endY + 1;
    }

    public static double waterLevel(EntityBoat boat) {
        AxisAlignedBB hull = boat.getEntityBoundingBox();
        double level = -Double.MAX_VALUE;
        for (BlockPos pos : cells(hull, MathHelper.floor(hull.minY), MathHelper.ceil(hull.minY + CONTACT_DEPTH))) {
            IBlockState fluid = Waterlogging.fluidState(boat.world, pos);
            float height = waterHeight(boat.world, pos, fluid, hull);
            if (height > 0) level = Math.max(level, pos.getY() + (double) height);
        }
        return level;
    }

    public static boolean touchesWater(EntityBoat boat, double level) {
        return boat.getEntityBoundingBox().minY < level;
    }

    @Nullable
    public static EntityBoat.Status underwaterStatus(EntityBoat boat) {
        AxisAlignedBB hull = boat.getEntityBoundingBox();
        double top = hull.maxY + CONTACT_DEPTH;
        boolean underSource = false;
        for (BlockPos pos : cells(hull, MathHelper.floor(hull.maxY), MathHelper.ceil(top))) {
            IBlockState fluid = Waterlogging.fluidState(boat.world, pos);
            float height = waterHeight(boat.world, pos, fluid, hull);
            if (height <= 0 || top >= pos.getY() + (double) height) continue;
            if (!isSource(boat.world, pos, fluid)) return EntityBoat.Status.UNDER_FLOWING_WATER;
            underSource = true;
        }
        return underSource ? EntityBoat.Status.UNDER_WATER : null;
    }

    private static float waterHeight(World world, BlockPos pos, IBlockState fluid, AxisAlignedBB sample) {
        Block block = fluid.getBlock();
        if (block instanceof BlockLiquid || block instanceof PhysicalFluidBlock) {
            return Waterlogging.isWater(world, pos, fluid) ? Waterlogging.liquidShapeHeight(world, pos, fluid) : 0;
        }
        Boolean containsWater = block.isAABBInsideMaterial(world, pos, sample, Material.WATER);
        return Boolean.TRUE.equals(containsWater) ? block.getBlockLiquidHeight(world, pos, fluid, Material.WATER) : 0;
    }

    private static boolean isSource(World world, BlockPos pos, IBlockState fluid) {
        Block block = fluid.getBlock();
        if (block instanceof BlockLiquid) return fluid.getValue(BlockLiquid.LEVEL) == 0;
        if (block instanceof PhysicalFluidBlock) return fluid.getValue(BlockFluidBase.LEVEL) == 0;
        return block instanceof IFluidBlock && ((IFluidBlock) block).canDrain(world, pos);
    }

    private static Iterable<BlockPos.MutableBlockPos> cells(AxisAlignedBB hull, int firstY, int endY) {
        return BlockPos.getAllInBoxMutable(MathHelper.floor(hull.minX), firstY, MathHelper.floor(hull.minZ),
                MathHelper.ceil(hull.maxX) - 1, endY - 1, MathHelper.ceil(hull.maxZ) - 1);
    }
}
