package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.EntityJuniperBoat;
import ivorius.psychedelicraft.entity.EntityJuniperChestBoat;
import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.IFluidBlock;

import javax.annotation.Nullable;

public final class JuniperBoatItem extends Item {
    private final boolean chest;

    public JuniperBoatItem(boolean chest) {
        this.chest = chest;
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        Vec3d eyes = player.getPositionEyes(1);
        Vec3d look = Vec3d.fromPitchYaw(player.rotationPitch, player.rotationYaw).scale(5);
        Vec3d hit = launchPoint(world, eyes, eyes.add(look));
        if (hit == null) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(player,
                player.getEntityBoundingBox().expand(look.x, look.y, look.z).grow(1))) {
            if (entity.canBeCollidedWith() && entity.getEntityBoundingBox()
                    .grow(entity.getCollisionBorderSize()).contains(eyes)) {
                return new ActionResult<>(EnumActionResult.PASS, stack);
            }
        }

        EntityJuniperBoat boat = createBoat(world, hit.x, hit.y, hit.z, player.rotationYaw);
        if (!world.getCollisionBoxes(boat, boat.getEntityBoundingBox().grow(-0.1)).isEmpty()) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!world.isRemote) {
            if (!world.spawnEntity(boat)) {
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            player.addStat(StatList.getObjectUseStats(this));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Nullable
    private static Vec3d launchPoint(World world, Vec3d start, Vec3d end) {
        RayTraceResult blockHit = world.rayTraceBlocks(start, end);
        Vec3d nearest = blockHit == null ? null : blockHit.hitVec;
        Vec3d limit = nearest == null ? end : nearest;
        Vec3d inside = start.add(end.subtract(start).scale(0.001));
        // Forge's liquid ray trace only hits source blocks. Compare fluid shapes against the native outline hit.
        for (BlockPos pos : BlockPos.getAllInBoxMutable(new BlockPos(start), new BlockPos(limit))) {
            double height = fluidHeight(world, pos);
            if (height == 0) continue;
            double bottom = pos.getY() + (height < 0 ? 1 + height : 0);
            AxisAlignedBB volume = new AxisAlignedBB(pos.getX(), bottom, pos.getZ(),
                    pos.getX() + 1, bottom + Math.abs(height), pos.getZ() + 1);
            Vec3d point = inside;
            if (!volume.contains(inside)) {
                RayTraceResult hit = volume.calculateIntercept(start, end);
                if (hit == null) continue;
                point = hit.hitVec;
            }
            if (nearest == null || start.squareDistanceTo(point) < start.squareDistanceTo(nearest)) {
                nearest = point;
            }
        }
        return nearest;
    }

    private static double fluidHeight(World world, BlockPos pos) {
        IBlockState state = Waterlogging.fluidState(world, pos);
        Block block = state.getBlock();
        if (block instanceof BlockLiquid) {
            IBlockState above = Waterlogging.fluidState(world, pos.up());
            return above.getMaterial() == state.getMaterial() ? 1
                    : 1 - BlockLiquid.getLiquidHeightPercent(state.getValue(BlockLiquid.LEVEL));
        }
        if (block instanceof PhysicalFluidBlock) {
            return world.getBlockState(pos.up()).getBlock() == block ? 1
                    : (8 - state.getValue(BlockFluidBase.LEVEL)) / 9F;
        }
        return block instanceof IFluidBlock ? ((IFluidBlock) block).getFilledPercentage(world, pos) : 0;
    }

    public void registerDispenserBehavior() {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos front = source.getBlockPos().offset(facing);
                boolean waterInFront = Waterlogging.isWater(world, front);
                if (!waterInFront && (!world.isAirBlock(front) || !Waterlogging.isWater(world, front.down()))) {
                    return super.dispenseStack(source, stack);
                }
                double y = source.getY() + facing.getFrontOffsetY() * 1.125;
                if (waterInFront) {
                    y++;
                }
                EntityJuniperBoat boat = createBoat(world, source.getX() + facing.getFrontOffsetX() * 1.125,
                        y, source.getZ() + facing.getFrontOffsetZ() * 1.125, facing.getHorizontalAngle());
                if (world.spawnEntity(boat)) {
                    stack.shrink(1);
                }
                return stack;
            }
        });
    }

    private EntityJuniperBoat createBoat(World world, double x, double y, double z, float yaw) {
        EntityJuniperBoat boat = chest ? new EntityJuniperChestBoat(world) : new EntityJuniperBoat(world);
        boat.setLocationAndAngles(x, y, z, yaw, 0);
        return boat;
    }
}
