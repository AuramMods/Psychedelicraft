package ivorius.psychedelicraft.entity;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Random;

final class AddictStrollTask extends EntityAIBase {
    private static final int HORIZONTAL_RANGE = 8;
    private static final int VERTICAL_RANGE = 6;
    private static final int STROLL_INTERVAL = 180;
    private final EntityVillager villager;
    private long nextStroll;
    private BlockPos destination;

    AddictStrollTask(EntityVillager villager) {
        this.villager = villager;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        long now = villager.world.getTotalWorldTime();
        if (!canStroll() || now <= nextStroll || villager.world.getClosestPlayerToEntity(villager, 9) == null) return false;
        nextStroll = now + STROLL_INTERVAL;
        destination = findDestination();
        return destination != null && VillagerWork.manhattan(villager.getPosition(), destination) > 1;
    }

    @Nullable
    private BlockPos findDestination() {
        Random random = villager.getRNG();
        PathNavigate navigation = villager.getNavigator();
        BlockPos origin = villager.getPosition();
        boolean hasHome = villager.hasHome();
        BlockPos home = villager.getHomePosition();
        double homeRange = villager.getMaximumHomeDistance() + HORIZONTAL_RANGE + 1;
        boolean restricted = hasHome && villager.getDistanceSqToCenter(home) < homeRange * homeRange;
        float bestWeight = Float.NEGATIVE_INFINITY;
        BlockPos best = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = random.nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE;
            int y = random.nextInt(VERTICAL_RANGE * 2 + 1) - VERTICAL_RANGE;
            int z = random.nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE;
            if (hasHome) {
                x += (villager.posX > home.getX() ? -1 : 1) * random.nextInt(HORIZONTAL_RANGE / 2);
                z += (villager.posZ > home.getZ() ? -1 : 1) * random.nextInt(HORIZONTAL_RANGE / 2);
            }
            BlockPos pos = origin.add(x, y, z);
            if (pos.getY() <= 0 || pos.getY() >= villager.world.getHeight()
                    || !villager.world.isBlockLoaded(pos) || (restricted && !villager.isWithinHomeDistanceFromPosition(pos))
                    || !navigation.canEntityStandOnPos(pos)) continue;
            PathNodeType type = navigation.getNodeProcessor().getPathNodeType(villager.world, pos.getX(), pos.getY(), pos.getZ());
            if (villager.getPathPriority(type) != 0F) continue;
            float weight = villager.getBlockPathWeight(pos);
            if (weight > bestWeight) {
                best = pos;
                bestWeight = weight;
            }
        }
        return best;
    }

    @Override
    public void startExecuting() {
        villager.getNavigator().tryMoveToXYZ(destination.getX() + 0.5D, destination.getY(),
                destination.getZ() + 0.5D, 0.5D);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return canStroll() && villager.world.getTotalWorldTime() <= nextStroll && !villager.getNavigator().noPath()
                && VillagerWork.manhattan(villager.getPosition(), destination) > 1;
    }

    @Override
    public void resetTask() {
        villager.getNavigator().clearPath();
    }

    private boolean canStroll() {
        return villager.getProfessionForge() == VillagerCareers.ADDICT && VillagerWork.canWork(villager);
    }
}
