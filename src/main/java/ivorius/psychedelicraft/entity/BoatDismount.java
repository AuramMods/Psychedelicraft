package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import java.util.ArrayList;
import java.util.List;

public final class BoatDismount {
    private BoatDismount() {
    }

    public static boolean placePassenger(EntityLivingBase passenger, Entity vehicle) {
        if (!(vehicle instanceof EntityJuniperBoat)) return false;
        double distance = (vehicle.width * MathHelper.SQRT_2 + passenger.width + 0.00001) / 2;
        float yaw = passenger.rotationYaw * ((float) Math.PI / 180);
        float xOffset = -MathHelper.sin(yaw);
        float zOffset = MathHelper.cos(yaw);
        double scale = distance / Math.max(Math.abs(xOffset), Math.abs(zOffset));
        double x = vehicle.posX + xOffset * scale;
        double z = vehicle.posZ + zOffset * scale;
        double hullTop = vehicle.getEntityBoundingBox().maxY;
        BlockPos upper = new BlockPos(x, hullTop, z);
        World world = passenger.world;
        if (!Waterlogging.isWater(world, upper.down())) {
            double upperFloor = floorHeight(world, upper);
            double lowerFloor = floorHeight(world, upper.down());
            boolean playerCanStand = passenger instanceof EntityPlayer && !passenger.isElytraFlying();
            float height = playerCanStand ? 1.8F : passenger.height;
            if (tryFloor(passenger, x, z, upperFloor, height) || tryFloor(passenger, x, z, lowerFloor, height)) {
                return true;
            }
            if (playerCanStand && (tryFloor(passenger, x, z, upperFloor, 1.65F)
                    || tryFloor(passenger, x, z, lowerFloor, 1.65F))) {
                passenger.setSneaking(true);
                passenger.height = 1.65F;
                passenger.setPosition(x, passenger.posY, z);
                return true;
            }
        }
        passenger.setPosition(vehicle.posX, hullTop, vehicle.posZ);
        return true;
    }

    private static double floorHeight(World world, BlockPos pos) {
        double top = collisionTop(world, pos);
        if (top == Double.NEGATIVE_INFINITY) {
            top = collisionTop(world, pos.down());
            if (top < pos.getY()) return Double.NaN;
        }
        return top < pos.getY() + 1 ? top : Double.NaN;
    }

    private static double collisionTop(World world, BlockPos pos) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        world.getBlockState(pos).addCollisionBoxToList(world, pos, new AxisAlignedBB(pos).grow(0, 1, 0),
                boxes, null, false);
        double top = Double.NEGATIVE_INFINITY;
        for (AxisAlignedBB box : boxes) top = Math.max(top, box.maxY);
        return top;
    }

    private static boolean tryFloor(EntityLivingBase passenger, double x, double z, double y, float height) {
        if (!Double.isFinite(y)) return false;
        double halfWidth = passenger.width / 2.0;
        AxisAlignedBB bounds = new AxisAlignedBB(x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + height, z + halfWidth);
        World world = passenger.world;
        WorldBorder border = world.getWorldBorder();
        if (bounds.minX < border.minX() || bounds.maxX > border.maxX()
                || bounds.minZ < border.minZ() || bounds.maxZ > border.maxZ()
                || world.collidesWithAnyBlock(bounds)) return false;
        passenger.setPosition(x, y, z);
        return true;
    }
}
