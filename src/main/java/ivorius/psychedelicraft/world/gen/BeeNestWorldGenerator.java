package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;

final class BeeNestWorldGenerator {
    private static final int SIDE = 16;
    private static final int COLUMNS = SIDE * SIDE;

    private BeeNestWorldGenerator() {
    }

    static void decorate(World world, ChunkPos chunk, Random random, PSConfig.Feature options) {
        Biome biome = world.getBiome(chunk.getBlock(16, 0, 16));
        if (!PlantBiomes.allows(options, BeeNestWorldGenerator::defaultBiome, biome)) return;

        int rarity = biome == Biomes.PLAINS || biome == Biomes.MUTATED_PLAINS ? 2
                : biome == Biomes.MUTATED_FOREST ? 4 : 16;
        if (random.nextInt(rarity) != 0) return;

        // An odd step visits each of the 256 columns once without allocating a shuffled list.
        int first = random.nextInt(COLUMNS);
        int step = 2 * random.nextInt(COLUMNS / 2) + 1;
        for (int attempt = 0; attempt < COLUMNS; attempt++) {
            int index = (first + attempt * step) % COLUMNS;
            BlockPos column = chunk.getBlock(8 + index % SIDE, 0, 8 + index / SIDE);
            if (!PlantBiomes.allows(options, BeeNestWorldGenerator::defaultBiome, world.getBiome(column))) continue;
            if (placeOnTree(world, world.getHeight(column), random)) return;
        }
    }

    private static boolean defaultBiome(Biome biome) {
        return biome == Biomes.PLAINS || biome == Biomes.MUTATED_PLAINS || biome == Biomes.MUTATED_FOREST
                || biome == Biomes.FOREST || biome == Biomes.FOREST_HILLS || biome == Biomes.BIRCH_FOREST
                || biome == Biomes.BIRCH_FOREST_HILLS || biome == Biomes.MUTATED_BIRCH_FOREST
                || biome == Biomes.MUTATED_BIRCH_FOREST_HILLS;
    }

    private static boolean placeOnTree(World world, BlockPos top, Random random) {
        int first = random.nextInt(4);
        for (int depth = 1; depth <= 8 && depth < top.getY(); depth++) {
            BlockPos trunk = top.down(depth);
            IBlockState state = world.getBlockState(trunk);
            if (state.getBlock() != Blocks.LOG) continue;
            BlockPlanks.EnumType wood = state.getValue(BlockOldLog.VARIANT);
            if (wood != BlockPlanks.EnumType.OAK && wood != BlockPlanks.EnumType.BIRCH) continue;
            for (int index = 0; index < 4; index++) {
                BlockPos nest = trunk.offset(EnumFacing.getHorizontal((first + index) % 4));
                if (world.isAirBlock(nest) && world.getBlockState(nest.up()).getBlock() instanceof BlockLeaves
                        && world.setBlockState(nest, PSBlocks.BEE_NEST.getDefaultState(), 2)) {
                    return true;
                }
            }
        }
        return false;
    }
}
