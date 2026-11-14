package ivorius.psychedelicraft.world.gen;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import ivorius.psychedelicraft.block.JuniperLeavesBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.BlockDirt;
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
        if (base.getY() < 1 || base.getY() + height + 1 > world.getActualHeight()
                || !world.isAreaLoaded(base.add(-6, 0, -6), base.add(6, 0, 6))
                || !PSBlocks.JUNIPER_SAPLING.canBlockStay(world, base, PSBlocks.JUNIPER_SAPLING.getDefaultState())) {
            return false;
        }

        for (int y = 0; y <= height + 1; y++) {
            int radius = y == 0 ? 0 : 2;
            for (BlockPos pos : BlockPos.getAllInBox(base.add(-radius, y, -radius), base.add(radius, y, radius))) {
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock() == Blocks.VINE || (!canPlaceLog(world, base, pos)
                        && !state.getBlock().canSustainLeaves(state, world, pos))) {
                    return false;
                }
            }
        }

        EnumFacing first = EnumFacing.Plane.HORIZONTAL.random(random);
        int bend = height - random.nextInt(4) - 1;
        Set<BlockPos> logs = new LinkedHashSet<>();
        BlockPos top = addBranch(world, logs, base, first, 0, height, bend, 3 - random.nextInt(3));
        Set<BlockPos> leaves = new LinkedHashSet<>();
        EnumFacing second = EnumFacing.Plane.HORIZONTAL.random(random);
        BlockPos fork = null;
        if (first != second) {
            int start = bend - random.nextInt(2) - 1;
            int end = Math.min(height, start + 1 + random.nextInt(3));
            int firstLevel = Math.max(1, start);
            fork = addBranch(world, logs, base, second, firstLevel, end, firstLevel, end - firstLevel);
        }
        if (logs.isEmpty()) {
            return false;
        }
        if (top != null) {
            addCanopy(world, random, leaves, top, 1);
        }
        if (fork != null) {
            addCanopy(world, random, leaves, fork, 0);
        }
        leaves.removeAll(logs);

        world.setBlockState(base.down(), Blocks.DIRT.getDefaultState()
                .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT), 2);
        IBlockState log = PSBlocks.JUNIPER_LOG.getDefaultState();
        for (BlockPos pos : logs) {
            world.setBlockState(pos, log, 2);
        }
        Set<BlockPos> frontier = logs;
        for (int distance = 1; distance < JuniperLeavesBlock.UNSUPPORTED && !frontier.isEmpty(); distance++) {
            Set<BlockPos> next = new LinkedHashSet<>();
            for (BlockPos pos : frontier) {
                for (EnumFacing face : EnumFacing.VALUES) {
                    BlockPos adjacent = pos.offset(face);
                    if (leaves.remove(adjacent)) {
                        placeLeaf(world, adjacent, distance);
                        next.add(adjacent);
                    }
                }
            }
            frontier = next;
        }
        for (BlockPos pos : leaves) {
            placeLeaf(world, pos, JuniperLeavesBlock.UNSUPPORTED);
        }
        return true;
    }

    private static BlockPos addBranch(World world, Set<BlockPos> logs, BlockPos base, EnumFacing direction,
                                      int start, int end, int bend, int steps) {
        BlockPos column = base;
        int canopyHeight = 0;
        for (int y = start; y < end; y++) {
            if (y >= bend && steps > 0) {
                column = column.offset(direction);
                steps--;
            }
            BlockPos log = column.up(y);
            if (!logs.contains(log) && canPlaceLog(world, base, log)) {
                logs.add(log);
                canopyHeight = y + 1;
            }
        }
        return canopyHeight == 0 ? null : column.up(canopyHeight);
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
                    if (PlantWorldGenerator.replaceableByTrees(world, pos)) {
                        leaves.add(pos);
                    }
                }
            }
        }
    }

    private static boolean canPlaceLog(World world, BlockPos base, BlockPos pos) {
        return (base.equals(pos) && world.getBlockState(pos).getBlock() == PSBlocks.JUNIPER_SAPLING)
                || PlantWorldGenerator.replaceableByTrees(world, pos);
    }

    private static void placeLeaf(World world, BlockPos pos, int distance) {
        IBlockState previous = world.getBlockState(pos);
        boolean waterlogged = Waterlogging.isWaterSource(previous) || Waterlogging.isWet(world, pos, previous);
        IBlockState leaf = PSBlocks.JUNIPER_LEAVES.getDefaultState().withProperty(JuniperLeavesBlock.DISTANCE, distance);
        world.setBlockState(pos, leaf, 2);
        if (waterlogged) {
            Waterlogging.fillSource(world, pos);
        }
    }
}
