package ivorius.psychedelicraft.entity.drug;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

final class MushroomGrowth {
    private MushroomGrowth() {}

    static void tick(Player player, DrugProperties properties) {
        double strength = strength(properties);
        if (strength <= 0 || !player.isAlive() || player.isSpectator()) return;
        ServerLevel world = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        int radius = radius(strength);
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos position : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (position.distSqr(center) > radius * radius || world.isOutsideBuildHeight(position) || !world.hasChunkAt(position)) continue;
            if (world.getBlockState(position).isRandomlyTicking()) targets.add(position.immutable());
        }
        int remaining = tickCount(strength);
        while (remaining > 0 && !targets.isEmpty()) {
            int index = world.random.nextInt(targets.size());
            BlockPos position = targets.get(index);
            BlockState state = world.getBlockState(position);
            if (state.isRandomlyTicking()) {
                state.randomTick(world, position, world.random);
                remaining--;
            } else {
                targets.set(index, targets.get(targets.size() - 1));
                targets.remove(targets.size() - 1);
            }
        }
    }

    static double strength(DrugProperties properties) {
        return Math.min(1, properties.getActiveValue(DrugType.RED_SHROOMS) + properties.getActiveValue(DrugType.BROWN_SHROOMS));
    }
    static int radius(double strength) { return strength > 0 ? 2 + Mth.floor(3 * growthScale(strength)) : 0; }
    static int tickCount(double strength) { return strength > 0 ? 3 + Mth.floor(15 * growthScale(strength)) : 0; }
    private static double growthScale(double strength) { return Mth.clamp((strength - 0.4) / 0.6, 0, 1); }
}
