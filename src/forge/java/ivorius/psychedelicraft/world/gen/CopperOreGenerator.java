package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public final class CopperOreGenerator implements IWorldGenerator {
    private final WorldGenMinable ore = new WorldGenMinable(PSBlocks.COPPER_ORE.getDefaultState(), 10);

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator generator, IChunkProvider provider) {
        if (!PSConfig.balancing.worldGeneration.copperOre || world.provider.getDimension() != 0
                || world.getWorldType() == WorldType.FLAT) return;
        for (int vein = 0; vein < 16; vein++) {
            int x = chunkX * 16 + random.nextInt(16);
            int y = random.nextInt(64) + random.nextInt(64) - 15;
            int z = chunkZ * 16 + random.nextInt(16);
            if (y > 0) ore.generate(world, random, new BlockPos(x, y, z));
        }
    }
}
