package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.entity.SnifferEntity.Activity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;

import java.util.Random;

final class SnifferDigGoal extends EntityAIBase {
    private final SnifferEntity sniffer;
    private BlockPos soil;
    private Path path;
    private int duration;
    private int idleDelay;
    private boolean foundSeed;

    SnifferDigGoal(SnifferEntity sniffer) {
        this.sniffer = sniffer;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (idleDelay > 0) {
            idleDelay--;
            return false;
        }
        return sniffer.onGround && !sniffer.isInWater() && !sniffer.isInLove() && !sniffer.isRiding()
                && !sniffer.getLeashed() && sniffer.getRNG().nextInt(12) == 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return sniffer.activity() != Activity.IDLE && !sniffer.isInWater() && !sniffer.getLeashed()
                && !sniffer.isRiding() && !sniffer.isInLove();
    }

    @Override
    public void startExecuting() {
        boolean search = sniffer.readyToDig() && sniffer.getRNG().nextBoolean();
        transition(search ? Activity.SNIFFING : Activity.SCENTING, 40 + sniffer.getRNG().nextInt(41),
                search ? PSSounds.SNIFFER_SNIFFING : PSSounds.SNIFFER_SCENTING);
    }

    @Override
    public void resetTask() {
        sniffer.getNavigator().clearPath();
        sniffer.setActivity(Activity.IDLE);
        soil = null;
        path = null;
        idleDelay = 10;
        foundSeed = false;
    }

    @Override
    public void updateTask() {
        int elapsed = sniffer.activityTicks();
        switch (sniffer.activity()) {
            case SNIFFING:
                if (elapsed >= duration) {
                    if (findSoil() && sniffer.getNavigator().setPath(path, 1.25)) {
                        transition(Activity.SEARCHING, 600, null);
                    } else {
                        sniffer.setActivity(Activity.IDLE);
                    }
                }
                break;
            case SEARCHING:
                if (soil == null || !sniffer.canDig(soil) || elapsed >= duration) {
                    sniffer.setActivity(Activity.IDLE);
                } else if (sniffer.getNavigator().noPath()) {
                    if (sniffer.getDistanceSqToCenter(soil.up()) < 9 && sniffer.readyToDig()) {
                        transition(Activity.DIGGING, 160 + sniffer.getRNG().nextInt(21), PSSounds.SNIFFER_DIGGING);
                    } else {
                        sniffer.setActivity(Activity.IDLE);
                    }
                } else if (elapsed % 20 == 0) {
                    sniffer.playSound(PSSounds.SNIFFER_SEARCHING, 1F, 1F);
                }
                break;
            case DIGGING:
                turnToSoil();
                if (!sniffer.onGround) break;
                if (!foundSeed && !sniffer.canDig(soil)) {
                    sniffer.setActivity(Activity.IDLE);
                } else if (!foundSeed && elapsed >= 120) {
                    dropSeed();
                    sniffer.recordDig(soil);
                    foundSeed = true;
                } else if (elapsed >= duration) {
                    transition(Activity.RISING, 40, PSSounds.SNIFFER_DIGGING_STOP);
                }
                break;
            case RISING:
                if (elapsed >= duration) transition(Activity.HAPPY, 40 + sniffer.getRNG().nextInt(61), PSSounds.SNIFFER_HAPPY);
                break;
            case SCENTING:
            case HAPPY:
                if (elapsed >= duration) sniffer.setActivity(Activity.IDLE);
                break;
            default:
                break;
        }
    }

    private void transition(Activity activity, int ticks, SoundEvent sound) {
        sniffer.setActivity(activity);
        duration = ticks;
        if (sound != null) sniffer.playSound(sound, 1F, sniffer.isChild() ? 1.3F : 1F);
    }

    private boolean findSoil() {
        Random random = sniffer.getRNG();
        BlockPos feet = new BlockPos(sniffer);
        for (int i = 0; i < 20; i++) {
            int radius = 10 + 2 * (i / 4);
            BlockPos column = feet.add(random.nextInt(2 * radius + 1) - radius, 2, random.nextInt(2 * radius + 1) - radius);
            for (int y = 0; y <= 6; y++) {
                BlockPos candidate = column.down(y);
                if (!sniffer.canDig(candidate) || !sniffer.world.isAirBlock(candidate.up())) continue;
                if (findApproach(candidate)) {
                    soil = candidate;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findApproach(BlockPos candidate) {
        int first = sniffer.getRNG().nextInt(4);
        for (int i = 0; i < 4; i++) {
            BlockPos stand = candidate.up().offset(EnumFacing.getHorizontal((first + i) % 4), 2);
            if (!sniffer.world.isBlockLoaded(stand)) continue;
            Path route = sniffer.getNavigator().getPathToPos(stand);
            PathPoint last = route == null ? null : route.getFinalPathPoint();
            if (last != null && last.x == stand.getX() && last.z == stand.getZ() && Math.abs(last.y - stand.getY()) <= 1) {
                path = route;
                return true;
            }
        }
        return false;
    }

    private void turnToSoil() {
        double x = soil.getX() + .5 - sniffer.posX;
        double z = soil.getZ() + .5 - sniffer.posZ;
        float yaw = (float) (MathHelper.atan2(z, x) * (180D / Math.PI)) - 90F;
        sniffer.rotationYaw = yaw;
        sniffer.setRotationYawHead(yaw);
        sniffer.renderYawOffset = yaw;
    }

    private void dropSeed() {
        WorldServer world = (WorldServer) sniffer.world;
        LootContext context = new LootContext.Builder(world).withLootedEntity(sniffer).build();
        for (ItemStack stack : world.getLootTableManager().getLootTableFromLocation(SnifferEntity.DIGGING_LOOT)
                .generateLootForPools(sniffer.getRNG(), context)) {
            EntityItem drop = new EntityItem(world, soil.getX() + .5, soil.getY() + 1, soil.getZ() + .5, stack);
            drop.setDefaultPickupDelay();
            world.spawnEntity(drop);
        }
        sniffer.playSound(PSSounds.SNIFFER_DROP_SEED, 1F, 1F);
    }
}
