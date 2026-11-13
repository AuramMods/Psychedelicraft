package ivorius.psychedelicraft.world.gen;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import ivorius.psychedelicraft.block.JuniperLeavesBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class JuniperTree {
    private JuniperTree() {
    }

    public static boolean generate(World world, Random random, BlockPos base) {
        int height = 5 + random.nextInt(3) + random.nextInt(3);
        if (base.getY() < 1 || base.getY() + height + 1 >= world.getActualHeight()
                || !world.isAreaLoaded(base.add(-6, 0, -6), base.add(6, 0, 6))
                || !PSBlocks.JUNIPER_SAPLING.canBlockStay(world, base, PSBlocks.JUNIPER_SAPLING.getDefaultState())) {
            return false;
        }

        for (int y = 0; y <= height + 1; y++) {
            int radius = y == 0 ? 0 : 2;
            for (BlockPos pos : BlockPos.getAllInBox(base.add(-radius, y, -radius), base.add(radius, y, radius))) {
                if (!replaceable(world, pos)) {
                    return false;
                }
            }
        }

        EnumFacing first = EnumFacing.Plane.HORIZONTAL.random(random);
        int bend = height - random.nextInt(4) - 1;
        Set<BlockPos> logs = new LinkedHashSet<>();
        BlockPos top = addBranch(logs, base, first, 0, height, bend, 3 - random.nextInt(3));
        Set<BlockPos> leaves = new LinkedHashSet<>();
        EnumFacing second = EnumFacing.Plane.HORIZONTAL.random(random);
        BlockPos fork = null;
        if (first != second) {
            int start = bend - random.nextInt(2) - 1;
            int steps = 1 + random.nextInt(3);
            fork = addBranch(logs, base, second, Math.max(1, start), Math.min(height, start + steps),
                    Math.max(1, start), steps);
        }
        for (BlockPos log : logs) {
            if (!replaceable(world, log)) {
                return false;
            }
        }
        addCanopy(world, random, leaves, top.up(), 1);
        if (fork != null) {
            addCanopy(world, random, leaves, fork.up(), 0);
        }
        leaves.removeAll(logs);

        world.setBlockState(base.down(), Blocks.DIRT.getDefaultState()
                .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT), 2);
        IBlockState log = PSBlocks.JUNIPER_LOG.getDefaultState();
        for (BlockPos pos : logs) {
            world.setBlockState(pos, log, 2);
        }
        IBlockState leaf = PSBlocks.JUNIPER_LEAVES.getDefaultState();
        Set<BlockPos> frontier = logs;
        for (int distance = 1; distance < JuniperLeavesBlock.UNSUPPORTED && !frontier.isEmpty(); distance++) {
            Set<BlockPos> next = new LinkedHashSet<>();
            for (BlockPos pos : frontier) {
                for (EnumFacing face : EnumFacing.VALUES) {
                    BlockPos adjacent = pos.offset(face);
                    if (leaves.remove(adjacent)) {
                        world.setBlockState(adjacent, leaf.withProperty(JuniperLeavesBlock.DISTANCE, distance), 2);
                        next.add(adjacent);
                    }
                }
            }
            frontier = next;
        }
        for (BlockPos pos : leaves) {
            world.setBlockState(pos, leaf, 2);
        }
        return true;
    }

    private static BlockPos addBranch(Set<BlockPos> logs, BlockPos base, EnumFacing direction,
                                      int start, int end, int bend, int steps) {
        BlockPos column = base;
        BlockPos top = null;
        for (int y = start; y < end; y++) {
            if (y >= bend && steps > 0) {
                column = column.offset(direction);
                steps--;
            }
            top = column.up(y);
            logs.add(top);
        }
        return top;
    }

    private static void addCanopy(World world, Random random, Set<BlockPos> leaves, BlockPos center, int extraRadius) {
        for (int y = 0; y >= -3; y--) {
            int radius = 1 + extraRadius - y / 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius && Math.abs(z) == radius && (random.nextInt(2) == 0 || y == 0)) {
                        continue;
                    }
                    BlockPos pos = center.add(x, y, z);
                    if (replaceable(world, pos)) {
                        leaves.add(pos);
                    }
                }
            }
        }
    }

    private static boolean replaceable(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        return material == Material.AIR || material == Material.PLANTS || material == Material.VINE
                || state.getBlock().isLeaves(state, world, pos);
    }
}
