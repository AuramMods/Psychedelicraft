package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.AgePlantBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDesert;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class DesertVillageDecoration {
    private static final int AGAVE_WEIGHT = 10;
    private static final int PEYOTE_WEIGHT = 1;
    private static final int VANILLA_DECOR_WEIGHT = 28;
    private static final long DECOR_SALT = 0x7162E234A981C5D3L;

    private DesertVillageDecoration() {
    }

    @SubscribeEvent
    public static void populate(PopulateChunkEvent.Post event) {
        World world = event.getWorld();
        if (!event.isHasVillageGenerated() || world.isRemote
                || world.provider.getDimensionType() != DimensionType.OVERWORLD) {
            return;
        }
        ChunkPos chunk = new ChunkPos(event.getChunkX(), event.getChunkZ());
        Random random = new Random(world.getSeed() ^ DECOR_SALT ^ (341873128712L * chunk.x + 132897987541L * chunk.z));
        AgePlantBlock plant = choosePlant(random.nextInt(VANILLA_DECOR_WEIGHT + AGAVE_WEIGHT + PEYOTE_WEIGHT));
        if (plant == null) {
            return;
        }

        // A radius-seven patch around this anchor stays inside this population area's 16 columns per axis.
        BlockPos anchor = chunk.getBlock(15 + random.nextInt(2), 0, 15 + random.nextInt(2));
        if (world.getBiome(anchor) instanceof BiomeDesert) {
            BlockPos surface = PlantWorldGenerator.motionBlockingSurface(world, anchor);
            PlantWorldGenerator.placeLowPatch(world, surface, random, plant, plant.maxAge());
        }
    }

    @Nullable
    static AgePlantBlock choosePlant(int roll) {
        if (roll < AGAVE_WEIGHT) {
            return PSBlocks.AGAVE_PLANT;
        }
        return roll < AGAVE_WEIGHT + PEYOTE_WEIGHT ? PSBlocks.PEYOTE : null;
    }
}
