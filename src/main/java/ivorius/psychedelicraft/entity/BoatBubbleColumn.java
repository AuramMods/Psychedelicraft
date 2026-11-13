package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.BubbleColumnBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class BoatBubbleColumn {
    private static final DataParameter<Integer> SURFACE_TICKS =
            EntityDataManager.createKey(EntityBoat.class, DataSerializers.VARINT);
    private static final int CONTACT_TICKS = 60;
    private float strength;
    private float angle;
    private float previousAngle;

    public BoatBubbleColumn(EntityBoat boat) {
        boat.getDataManager().register(SURFACE_TICKS, 0);
    }

    public void tick(EntityBoat boat) {
        World world = boat.world;
        int remaining = boat.getDataManager().get(SURFACE_TICKS);
        if (world.isRemote) {
            strength = MathHelper.clamp(strength + (remaining > 0 ? 0.05F : -0.1F), 0, 1);
            previousAngle = angle;
            angle = 10 * MathHelper.sin(0.5F * world.getTotalWorldTime()) * strength;
        }

        AxisAlignedBB hull = boat.getEntityBoundingBox().shrink(0.001);
        BlockPos surface = null;
        boolean down = false;
        if (world.isAreaLoaded(new BlockPos(hull.minX, hull.minY, hull.minZ),
                new BlockPos(hull.maxX, hull.maxY, hull.maxZ))) {
            for (BlockPos pos : BlockPos.getAllInBoxMutable(new BlockPos(hull.minX, hull.minY, hull.minZ),
                    new BlockPos(hull.maxX, hull.maxY, hull.maxZ))) {
                IBlockState state = world.getBlockState(pos);
                if (!BubbleColumnBlock.isColumn(state)) continue;
                boolean drag = state.getValue(BubbleColumnBlock.DRAG_DOWN);
                if (world.isAirBlock(pos.up())) {
                    surface = pos.toImmutable();
                    down = drag;
                } else if (boat.canPassengerSteer()) {
                    BubbleColumnBlock.moveEntity(boat, drag, false);
                }
            }
        }
        if (surface != null) {
            BubbleColumnBlock.splash(world, surface);
            if (world.isRemote) {
                world.spawnParticle(EnumParticleTypes.WATER_SPLASH, boat.posX + world.rand.nextFloat(),
                        boat.posY + 0.7, boat.posZ + world.rand.nextFloat(), 0, 0, 0);
                if (world.rand.nextInt(20) == 0) {
                    world.playSound(boat.posX, boat.posY, boat.posZ, SoundEvents.ENTITY_GENERIC_SPLASH,
                            boat.getSoundCategory(), 1, 0.8F + 0.4F * world.rand.nextFloat(), false);
                }
            }
        }
        if (world.isRemote) return;
        int next = surface == null ? 0 : (remaining == 0 ? CONTACT_TICKS : remaining) - 1;
        if (next != remaining) boat.getDataManager().set(SURFACE_TICKS, next);
        if (surface != null && next == 0) applyImpulse(boat, down);
    }

    private static void applyImpulse(EntityBoat boat, boolean down) {
        if (down) {
            boat.motionY -= 0.7;
            boat.removePassengers();
        } else {
            boolean hasPlayer = false;
            for (Entity passenger : boat.getPassengers()) hasPlayer |= passenger instanceof EntityPlayer;
            boat.motionY = hasPlayer ? 2.7 : 0.6;
        }
        // A mounted boat is moved by its controlling client in Forge 1.12.
        boat.velocityChanged = true;
    }

    public float angle(float partialTicks) {
        return previousAngle + (angle - previousAngle) * partialTicks;
    }

    public interface Carrier {
        BoatBubbleColumn psychedelicraft$bubbleColumn();
    }
}
