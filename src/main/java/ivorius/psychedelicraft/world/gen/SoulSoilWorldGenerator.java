package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import java.util.Random;

public final class SoulSoilWorldGenerator {
    private static final int NETHER_ROOF = 127;
    private static final int MAX_SURFACE_DEPTH = 5;

    private SoulSoilWorldGenerator() {
    }

    public static void buildSurfaces(World world, int chunkX, int chunkZ, ChunkPrimer primer) {
        if (world.provider.getDimensionType() != DimensionType.NETHER) return;
        Random seed = new Random(world.getSeed() ^ 0x534f554c534f494cL);
        NoiseGeneratorSimplex valley = new NoiseGeneratorSimplex(seed);
        NoiseGeneratorSimplex palette = new NoiseGeneratorSimplex(seed);
        NoiseGeneratorSimplex depth = new NoiseGeneratorSimplex(seed);
        for (int x = 0; x < 16; x++) {
            int worldX = (chunkX << 4) + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = (chunkZ << 4) + z;
                // Legacy Nether has one biome. Broad noise regions stand in for soul sand valleys.
                if (valley.getValue(worldX / 192D, worldZ / 192D) < 0.35D) continue;
                // The donor's palette selector samples its -4 octave in the horizontal plane.
                IBlockState surface = palette.getValue(worldX / 16D, worldZ / 16D) >= 0
                        ? Blocks.SOUL_SAND.getDefaultState() : PSBlocks.SOUL_SOIL.getDefaultState();
                int thickness = 4 + (int) Math.round(depth.getValue(worldX / 48D, worldZ / 48D));
                surfaceColumn(primer, x, z, surface, thickness);
            }
        }
    }

    private static void surfaceColumn(ChunkPrimer primer, int x, int z, IBlockState surface, int thickness) {
        int y = MAX_SURFACE_DEPTH;
        while (y <= NETHER_ROOF - MAX_SURFACE_DEPTH) {
            if (!isSurfaceStone(primer.getBlockState(x, y, z))) {
                y++;
                continue;
            }
            int bottom = y;
            do {
                y++;
            } while (y <= NETHER_ROOF - MAX_SURFACE_DEPTH && isSurfaceStone(primer.getBlockState(x, y, z)));
            if (isExposed(primer.getBlockState(x, bottom - 1, z))) {
                for (int layer = bottom; layer < Math.min(y, bottom + thickness); layer++) {
                    primer.setBlockState(x, layer, z, surface);
                }
            }
            if (isExposed(primer.getBlockState(x, y, z))) {
                for (int layer = Math.max(bottom, y - thickness); layer < y; layer++) {
                    primer.setBlockState(x, layer, z, surface);
                }
            }
        }
    }

    private static boolean isSurfaceStone(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.NETHERRACK || block == Blocks.SOUL_SAND;
    }

    private static boolean isExposed(IBlockState state) {
        return state.getMaterial() == Material.AIR || state.getMaterial() == Material.LAVA;
    }
}
