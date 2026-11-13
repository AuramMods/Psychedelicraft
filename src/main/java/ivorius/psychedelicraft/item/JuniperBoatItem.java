package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.EntityJuniperBoat;
import ivorius.psychedelicraft.entity.EntityJuniperChestBoat;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class JuniperBoatItem extends Item {
    private final boolean chest;

    public JuniperBoatItem(boolean chest) {
        this.chest = chest;
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        RayTraceResult hit = rayTrace(world, player, true);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        Vec3d eyes = player.getPositionEyes(1);
        Vec3d look = player.getLook(1).scale(5);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(player,
                player.getEntityBoundingBox().expand(look.x, look.y, look.z).grow(1))) {
            if (entity.canBeCollidedWith() && entity.getEntityBoundingBox()
                    .grow(entity.getCollisionBorderSize()).contains(eyes)) {
                return new ActionResult<>(EnumActionResult.PASS, stack);
            }
        }

        double surfaceOffset = world.getBlockState(hit.getBlockPos()).getMaterial() == Material.WATER ? 0.12 : 0;
        EntityJuniperBoat boat = createBoat(world, hit.hitVec.x, hit.hitVec.y - surfaceOffset,
                hit.hitVec.z, player.rotationYaw);
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

    public void registerDispenserBehavior() {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos front = source.getBlockPos().offset(facing);
                Material material = world.getBlockState(front).getMaterial();
                if (material != Material.WATER && (material != Material.AIR
                        || world.getBlockState(front.down()).getMaterial() != Material.WATER)) {
                    return super.dispenseStack(source, stack);
                }
                double y = source.getY() + facing.getFrontOffsetY() * 1.125;
                if (material == Material.WATER) {
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
