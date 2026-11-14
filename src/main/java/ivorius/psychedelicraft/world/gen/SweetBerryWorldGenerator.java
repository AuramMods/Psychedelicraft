package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.SweetBerryBushBlock;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class SweetBerryWorldGenerator {
    private static final int COMMON_RARITY = 32;
    private static final int SNOWY_RARITY = 384;
    private static final int ATTEMPTS = 96;
    private static final long SALT = 0x9e3779b97f4a7c15L * "sweet_berries".hashCode();

    private SweetBerryWorldGenerator() {
    }

    @SubscribeEvent
    public static void decorate(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        PSConfig.Feature options = PSConfig.balancing.worldGeneration.sweetBerries;
        if (!options.enabled || world.isRemote || world.provider.getDimensionType() != DimensionType.OVERWORLD) return;
        ChunkPos chunk = event.getChunkPos();
        // The offset keeps every candidate inside the four chunks available during Forge decoration.
        if (!world.isAreaLoaded(chunk.getBlock(0, 0, 0), chunk.getBlock(31, 0, 31))) return;
        long seed = world.getSeed() ^ (341873128712L * chunk.x + 132897987541L * chunk.z) ^ SALT;
        Random random = new Random(seed);
        float roll = random.nextFloat();
        if (roll >= 1F / COMMON_RARITY) return;
        BlockPos column = chunk.getBlock(8 + random.nextInt(16), 0, 8 + random.nextInt(16));
        Biome biome = world.getBiome(column);
        if (!PlantBiomes.allows(options, SweetBerryWorldGenerator::isTaiga, biome)
                || (isSnowyTaiga(biome) && roll >= 1F / SNOWY_RARITY)) return;

        BlockPos surface = world.getHeight(column);
        SweetBerryBushBlock bush = PSBlocks.SWEET_BERRY_BUSH;
        IBlockState mature = bush.getStateFromMeta(bush.maxAge());
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            BlockPos pos = surface.add(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
            if (pos.getY() > 0 && pos.getY() < world.getActualHeight()
                    && world.getBlockState(pos).getBlock() == Blocks.AIR
                    && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS) {
                world.setBlockState(pos, mature, 2);
            }
        }
    }

    private static boolean isTaiga(Biome biome) {
        return biome == Biomes.TAIGA || biome == Biomes.TAIGA_HILLS || biome == Biomes.MUTATED_TAIGA
                || biome == Biomes.REDWOOD_TAIGA || biome == Biomes.REDWOOD_TAIGA_HILLS
                || biome == Biomes.MUTATED_REDWOOD_TAIGA || biome == Biomes.MUTATED_REDWOOD_TAIGA_HILLS
                || isSnowyTaiga(biome);
    }

    private static boolean isSnowyTaiga(Biome biome) {
        return biome == Biomes.COLD_TAIGA || biome == Biomes.COLD_TAIGA_HILLS || biome == Biomes.MUTATED_TAIGA_COLD;
    }
}
