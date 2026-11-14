package ivorius.psychedelicraft.entity.drug;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class MushroomGrowth {
    private MushroomGrowth() {
    }

    static void tick(EntityPlayer player, DrugProperties properties) {
        double strength = strength(properties);
        if (strength <= 0 || !player.isEntityAlive() || player.isSpectator()) return;

        WorldServer world = (WorldServer) player.world;
        BlockPos center = player.getPosition();
        int radius = radius(strength);
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(
                center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (pos.distanceSq(center) > radius * radius || !world.isValid(pos) || !world.isBlockLoaded(pos)) continue;
            if (world.getBlockState(pos).getBlock().getTickRandomly()) targets.add(pos.toImmutable());
        }

        Random random = world.rand;
        int remaining = tickCount(strength);
        while (remaining > 0 && !targets.isEmpty()) {
            int index = random.nextInt(targets.size());
            BlockPos pos = targets.get(index);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock().getTickRandomly()) {
                state.getBlock().randomTick(world, pos, state, random);
                remaining--;
            } else {
                // A random tick can replace its own block or another target.
                targets.set(index, targets.get(targets.size() - 1));
                targets.remove(targets.size() - 1);
            }
        }
    }

    static double strength(DrugProperties properties) {
        return Math.min(1, properties.getActiveValue(DrugType.RED_SHROOMS) + properties.getActiveValue(DrugType.BROWN_SHROOMS));
    }

    static int radius(double strength) {
        return strength > 0 ? 2 + MathHelper.floor(3 * growthScale(strength)) : 0;
    }

    static int tickCount(double strength) {
        return strength > 0 ? 3 + MathHelper.floor(15 * growthScale(strength)) : 0;
    }

    private static double growthScale(double strength) {
        // A single ingested dose peaks just below 0.4 after decay during absorption.
        return MathHelper.clamp((strength - 0.4) / 0.6, 0, 1);
    }
}
