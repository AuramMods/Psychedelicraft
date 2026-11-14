package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class SmallFlowerWorldGenerator {
    private static final long SALT = 0x9e3779b97f4a7c15L * "small_flowers".hashCode();
    private static final NoiseGeneratorPerlin FLOWER_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    private SmallFlowerWorldGenerator() {}

    public static void registerGrassFlowers() {
        Biomes.PLAINS.addFlower(PSBlocks.CORNFLOWER.getDefaultState(), 20);
        Biomes.MUTATED_PLAINS.addFlower(PSBlocks.CORNFLOWER.getDefaultState(), 20);
        Biomes.MUTATED_FOREST.addFlower(PSBlocks.CORNFLOWER.getDefaultState(), 10);
        Biomes.MUTATED_FOREST.addFlower(PSBlocks.LILY_OF_THE_VALLEY.getDefaultState(), 10);
    }

    @SubscribeEvent
    public static void decorate(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimensionType() != DimensionType.OVERWORLD) return;
        ChunkPos chunk = event.getChunkPos();
        if (!world.isAreaLoaded(chunk.getBlock(0, 0, 0), chunk.getBlock(31, 0, 31))) return;
        Biome biome = world.getBiome(chunk.getBlock(16, 0, 16));

        long seed = world.getSeed() ^ (341873128712L * chunk.x + 132897987541L * chunk.z) ^ SALT;
        Random random = new Random(seed);
        if (biome == Biomes.PLAINS || biome == Biomes.MUTATED_PLAINS) {
            // The cornflower is one of four entries in the plains feature's high-noise flower pool.
            for (int attempt = 0; attempt < 4; attempt++) {
                if (random.nextInt(32) != 0) continue;
                BlockPos origin = surface(world, chunk, random);
                if (noise(origin, 0.005) < -0.8) continue;
                for (int spread = 0; spread < 64; spread++) {
                    BlockPos pos = offset(origin, random, 6, 2);
                    if (random.nextInt(12) == 0) plant(world, pos, biome, PSBlocks.CORNFLOWER);
                }
            }
        } else if (biome == Biomes.MUTATED_FOREST) {
            for (int attempt = 0; attempt < 3; attempt++) {
                if (random.nextBoolean()) {
                    BlockPos origin = surface(world, chunk, random);
                    for (int spread = 0; spread < 96; spread++) {
                        BlockPos pos = offset(origin, random, 6, 2);
                        int index = (int) (11 * MathHelper.clamp((1 + noise(pos, 1.0 / 48)) / 2, 0, 0.9999));
                        if (index == 9) plant(world, pos, biome, PSBlocks.CORNFLOWER);
                        if (index == 10) plant(world, pos, biome, PSBlocks.LILY_OF_THE_VALLEY);
                    }
                }
            }
            forestLilies(world, chunk, random, biome);
        } else if (isForest(biome)) {
            forestLilies(world, chunk, random, biome);
        }
    }

    private static boolean isForest(Biome biome) {
        return biome == Biomes.FOREST || biome == Biomes.FOREST_HILLS || biome == Biomes.BIRCH_FOREST
                || biome == Biomes.BIRCH_FOREST_HILLS || biome == Biomes.MUTATED_BIRCH_FOREST
                || biome == Biomes.MUTATED_BIRCH_FOREST_HILLS || biome == Biomes.ROOFED_FOREST
                || biome == Biomes.MUTATED_ROOFED_FOREST;
    }

    private static void forestLilies(World world, ChunkPos chunk, Random random, Biome biome) {
        if (random.nextInt(7) != 0) return;
        BlockPos origin = surface(world, chunk, random);
        int patches = random.nextInt(5) - 1;
        for (int patch = 0; patch < patches; patch++) {
            if (random.nextInt(4) != 0) continue;
            for (int spread = 0; spread < 96; spread++) {
                plant(world, offset(origin, random, 7, 3), biome, PSBlocks.LILY_OF_THE_VALLEY);
            }
        }
    }

    private static BlockPos surface(World world, ChunkPos chunk, Random random) {
        return PlantWorldGenerator.motionBlockingSurface(world, chunk.getBlock(8 + random.nextInt(16), 0, 8 + random.nextInt(16)));
    }

    private static BlockPos offset(BlockPos pos, Random random, int horizontal, int vertical) {
        return pos.add(random.nextInt(horizontal + 1) - random.nextInt(horizontal + 1),
                random.nextInt(vertical + 1) - random.nextInt(vertical + 1),
                random.nextInt(horizontal + 1) - random.nextInt(horizontal + 1));
    }

    private static double noise(BlockPos pos, double scale) {
        return FLOWER_NOISE.getValue(pos.getX() * scale, pos.getZ() * scale);
    }

    private static void plant(World world, BlockPos pos, Biome biome, BlockBush flower) {
        IBlockState state = flower.getDefaultState();
        if (pos.getY() > 0 && pos.getY() < world.getActualHeight() && world.getBiome(pos) == biome
                && world.isAirBlock(pos) && flower.canBlockStay(world, pos, state)) {
            world.setBlockState(pos, state, 2);
        }
    }
}
