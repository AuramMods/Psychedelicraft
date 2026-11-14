package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.MossSubstrateBlock;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public final class MossCaveWorldGenerator implements IWorldGenerator {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0 || world.getWorldType() == WorldType.FLAT) return;
        // Legacy terrain has no vertically indexed lush-cave biome. Select local patches below humid land.
        long caveRegionSeed = world.getSeed() ^ (Math.floorDiv(chunkX, 4) * 341873128712L
                + Math.floorDiv(chunkZ, 4) * 132897987541L);
        if (new Random(caveRegionSeed).nextInt(4) != 0) return;
        int x = (chunkX << 4) + 7;
        int z = (chunkZ << 4) + 7;
        Biome biome = world.getBiome(new BlockPos(x, 0, z));
        if (biome == Biomes.BEACH || biome.isSnowyBiome() || biome.getRainfall() < .7F
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) return;

        for (int attempt = 0; attempt < 20; attempt++) {
            int y = 10 + random.nextInt(38);
            BlockPos air = new BlockPos(x + random.nextInt(2), y, z + random.nextInt(2));
            if (!world.isAirBlock(air) || world.canSeeSky(air)) continue;
            for (int down = 0; down < 12 && world.isAirBlock(air.down()); down++) air = air.down();
            if (MossSubstrateBlock.placePatch(world, random, air, 5 + random.nextInt(3),
                    5 + random.nextInt(3), .3F, .8F)) return;
        }
    }
}
