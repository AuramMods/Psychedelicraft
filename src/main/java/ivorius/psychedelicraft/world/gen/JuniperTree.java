package ivorius.psychedelicraft.world.gen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
import net.minecraft.world.gen.structure.StructureBoundingBox;

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
        for (BlockPos pos : leaves) {
            placeLeaf(world, pos);
        }
        updateLeafDistances(world, base.down(), logs, leaves);
        return true;
    }

    private static void updateLeafDistances(World world, BlockPos roots, Set<BlockPos> logs, Set<BlockPos> leaves) {
        StructureBoundingBox bounds = new StructureBoundingBox(roots, roots);
        for (BlockPos pos : logs) bounds.expandTo(new StructureBoundingBox(pos, pos));
        for (BlockPos pos : leaves) bounds.expandTo(new StructureBoundingBox(pos, pos));

        List<Set<BlockPos>> pending = new ArrayList<>();
        for (int i = 0; i < JuniperLeavesBlock.UNSUPPORTED; i++) pending.add(new HashSet<>());
        pending.get(0).addAll(logs);
        Set<BlockPos> visited = new HashSet<>();
        visited.add(roots);
        int distance = 0;
        while (distance < pending.size()) {
            Iterator<BlockPos> positions = pending.get(distance).iterator();
            if (!positions.hasNext()) {
                distance++;
                continue;
            }
            BlockPos pos = positions.next();
            positions.remove();
            visited.add(pos);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof JuniperLeavesBlock && state.getValue(JuniperLeavesBlock.DISTANCE) != distance) {
                world.setBlockState(pos, state.withProperty(JuniperLeavesBlock.DISTANCE, distance), 2);
            }
            for (EnumFacing face : EnumFacing.VALUES) {
                BlockPos neighbor = pos.offset(face);
                if (!bounds.isVecInside(neighbor) || visited.contains(neighbor)) continue;
                int next = nextLeafDistance(world, neighbor, distance + 1);
                if (next < pending.size()) {
                    pending.get(next).add(neighbor);
                    // An existing support can lower the distance used for the rest of this step.
                    distance = Math.min(distance, next);
                }
            }
        }
    }

    private static int nextLeafDistance(World world, BlockPos pos, int fromTree) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().canSustainLeaves(state, world, pos)) return 0;
        if (state.getBlock() instanceof JuniperLeavesBlock) {
            return Math.min(fromTree, state.getValue(JuniperLeavesBlock.DISTANCE));
        }
        if (state.getBlock().isLeaves(state, world, pos)) {
            // Vanilla 1.12 foliage has decay flags in place of a saved support distance.
            return Math.min(fromTree, JuniperLeavesBlock.supportDistance(world, pos));
        }
        return JuniperLeavesBlock.UNSUPPORTED;
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

    private static void placeLeaf(World world, BlockPos pos) {
        IBlockState previous = world.getBlockState(pos);
        boolean waterlogged = Waterlogging.isWaterSource(previous) || Waterlogging.isWet(world, pos, previous);
        world.setBlockState(pos, PSBlocks.JUNIPER_LEAVES.getDefaultState(), 2);
        if (waterlogged) {
            Waterlogging.fillSource(world, pos);
        }
    }
}
