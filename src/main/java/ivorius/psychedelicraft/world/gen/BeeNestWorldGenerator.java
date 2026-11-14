package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.BeeNestBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class BeeNestWorldGenerator {
    private static final ThreadLocal<TreeBlocks> ACTIVE_TREE = new ThreadLocal<>();
    private static final EnumFacing[] NEST_SIDES = {EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
    private static final long NEST_SALT = 0x9e3779b97f4a7c15L;

    private BeeNestWorldGenerator() {
    }

    public static boolean generateNatural(WorldGenAbstractTree tree, World world, Random random, BlockPos base) {
        if (world.isRemote || world.provider.getDimensionType() != DimensionType.OVERWORLD || !supports(tree)) {
            return tree.generate(world, random, base);
        }
        PSConfig.Feature options = PSConfig.balancing.worldGeneration.beeNests;
        Biome biome = world.getBiome(base);
        if (!PlantBiomes.allows(options, candidate -> naturalChance(candidate) > 0, biome)) {
            return tree.generate(world, random, base);
        }
        float chance = naturalChance(biome);
        // Retain explicit biome overrides at the ordinary forest rate.
        Random nestRandom = new Random(world.getSeed() ^ (base.toLong() * NEST_SALT));
        if (nestRandom.nextFloat() >= (chance > 0 ? chance : 0.002F)) return tree.generate(world, random, base);
        return generateWithNest(tree, world, random, base, nestRandom);
    }

    public static boolean generateFromSapling(WorldGenerator tree, World world, Random random, BlockPos base) {
        if (!world.isRemote && PSConfig.balancing.worldGeneration.beeNests.enabled && supports(tree) && hasFlowers(world, base)) {
            Random nestRandom = new Random(world.getSeed() ^ (base.toLong() * NEST_SALT) ^ (world.getTotalWorldTime() * 31L));
            if (nestRandom.nextFloat() < 0.05F) return generateWithNest(tree, world, random, base, nestRandom);
        }
        return tree.generate(world, random, base);
    }

    private static boolean supports(WorldGenerator tree) {
        Class<?> type = tree.getClass();
        return type == WorldGenTrees.class || type == WorldGenBigTree.class || type == WorldGenBirchTree.class;
    }

    static float naturalChance(Biome biome) {
        if (biome == Biomes.PLAINS || biome == Biomes.MUTATED_PLAINS) return 0.05F;
        if (biome == Biomes.MUTATED_FOREST) return 0.02F;
        if (biome == Biomes.FOREST || biome == Biomes.FOREST_HILLS || biome == Biomes.BIRCH_FOREST
                || biome == Biomes.BIRCH_FOREST_HILLS || biome == Biomes.MUTATED_BIRCH_FOREST
                || biome == Biomes.MUTATED_BIRCH_FOREST_HILLS) return 0.002F;
        return 0F;
    }

    private static boolean hasFlowers(World world, BlockPos base) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(base.add(-2, -1, -2), base.add(2, 1, 2))) {
            if (world.isBlockLoaded(pos) && BeeNestBlock.isFlower(world.getBlockState(pos))) return true;
        }
        return false;
    }

    private static boolean generateWithNest(WorldGenerator tree, World world, Random random, BlockPos base, Random nestRandom) {
        TreeBlocks previous = ACTIVE_TREE.get();
        TreeBlocks blocks = new TreeBlocks(world);
        ACTIVE_TREE.set(blocks);
        boolean generated;
        try {
            generated = tree.generate(world, random, base);
        } finally {
            if (previous == null) ACTIVE_TREE.remove();
            else ACTIVE_TREE.set(previous);
        }
        if (generated) placeNest(world, blocks.logs, blocks.leaves, nestRandom);
        return generated;
    }

    public static boolean setTreeBlock(World world, BlockPos pos, IBlockState state, int flags) {
        boolean placed = world.setBlockState(pos, state, flags);
        TreeBlocks blocks = ACTIVE_TREE.get();
        if (!placed || blocks == null || blocks.world != world) return placed;
        blocks.logs.remove(pos);
        blocks.leaves.remove(pos);
        if (state.getBlock() == Blocks.LOG) {
            BlockPlanks.EnumType wood = state.getValue(BlockOldLog.VARIANT);
            if (wood == BlockPlanks.EnumType.OAK || wood == BlockPlanks.EnumType.BIRCH) blocks.logs.add(pos.toImmutable());
        } else if (state.getBlock() == Blocks.LEAVES) {
            blocks.leaves.add(pos.toImmutable());
        }
        return true;
    }

    static void placeNest(World world, Set<BlockPos> logs, Set<BlockPos> leaves, Random random) {
        if (logs.isEmpty()) return;
        int base = logs.stream().mapToInt(BlockPos::getY).min().getAsInt();
        int top = logs.stream().mapToInt(BlockPos::getY).max().getAsInt();
        int height = leaves.isEmpty() ? Math.min(base + 1 + random.nextInt(3), top)
                : Math.max(leaves.stream().mapToInt(BlockPos::getY).min().getAsInt() - 1, base + 1);
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos log : logs) {
            if (log.getY() != height) continue;
            for (EnumFacing side : NEST_SIDES) candidates.add(log.offset(side));
        }
        Collections.sort(candidates);
        Collections.shuffle(candidates, random);
        for (BlockPos nest : candidates) {
            BlockPos exit = nest.south();
            if (world.isBlockLoaded(nest) && world.isBlockLoaded(exit) && world.getBlockState(nest).getBlock() == Blocks.AIR
                    && world.getBlockState(exit).getBlock() == Blocks.AIR
                    && world.setBlockState(nest, PSBlocks.BEE_NEST.getDefaultState(), 2 | 16)) {
                PSBlocks.BEE_NEST.setFacing(world, nest, EnumFacing.SOUTH);
                return;
            }
        }
    }

    private static final class TreeBlocks {
        private final World world;
        private final Set<BlockPos> logs = new HashSet<>();
        private final Set<BlockPos> leaves = new HashSet<>();

        private TreeBlocks(World world) {
            this.world = world;
        }
    }
}
