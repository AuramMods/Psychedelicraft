package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.KelpBlock;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class KelpWorldGenerator {
    private static final NoiseGeneratorPerlin KELP_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    private KelpWorldGenerator() {
    }

    @SubscribeEvent
    public static void decorate(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        ChunkPos chunk = event.getChunkPos();
        PSConfig.Feature options = PSConfig.balancing.worldGeneration.kelp;
        if (!options.enabled || world.isRemote || world.provider.getDimensionType() != DimensionType.OVERWORLD) return;
        if (!world.isBlockLoaded(chunk.getBlock(0, 0, 0))) return;
        Chunk terrain = world.getChunkFromChunkCoords(chunk.x, chunk.z);
        int x = chunk.x << 4;
        int z = chunk.z << 4;
        int attempts = (int) Math.ceil(KELP_NOISE.getValue(x / 80D, z / 80D) * 120D);
        Random random = new Random(world.getSeed() ^ (341873128712L * chunk.x + 132897987541L * chunk.z) ^ 0x4b454c50L);
        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos column = chunk.getBlock(random.nextInt(16), 0, random.nextInt(16));
            if (PlantBiomes.allows(options, KelpWorldGenerator::isLegacyOcean, world.getBiome(column))) {
                place(world, terrain, column, random);
            }
        }
    }

    private static boolean isLegacyOcean(Biome biome) {
        return biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN;
    }

    private static void place(World world, Chunk terrain, BlockPos column, Random random) {
        BlockPos water = column.up(terrain.getHeightValue(column.getX() & 15, column.getZ() & 15) - 1);
        if (!Waterlogging.isWaterSource(terrain.getBlockState(water))) return;
        BlockPos root = water;
        while (root.getY() > 0 && Waterlogging.isWaterSource(terrain.getBlockState(root.down()))) root = root.down();
        if (root.equals(water) || terrain.getBlockState(root.down()).getBlock() instanceof KelpBlock
                || !KelpBlock.hasSupport(world, root)) return;
        int height = Math.min(2 + random.nextInt(10), water.getY() - root.getY());
        for (int segment = 0; segment < height - 1; segment++) {
            terrain.setBlockState(root.up(segment), KelpBlock.stem());
        }
        terrain.setBlockState(root.up(height - 1), KelpBlock.tip(20 + random.nextInt(4)));
    }
}
