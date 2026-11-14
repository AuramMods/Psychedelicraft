package ivorius.psychedelicraft.world.gen;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PlantSurfaceTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void treePlacementRequiresTheSurfaceToMatchTheOceanFloor() {
        ChunkPrimer primer = new ChunkPrimer();
        primer.setBlockState(0, 60, 0, Blocks.STONE.getDefaultState());
        primer.setBlockState(0, 61, 0, Blocks.TALLGRASS.getStateFromMeta(1));
        primer.setBlockState(1, 60, 0, Blocks.STONE.getDefaultState());
        primer.setBlockState(1, 61, 0, Blocks.WATER.getDefaultState());
        primer.setBlockState(2, 60, 0, Blocks.STONE.getDefaultState());
        primer.setBlockState(3, 60, 0, Blocks.LEAVES.getDefaultState());
        ColumnWorld world = new ColumnWorld(primer);

        assertNull(PlantWorldGenerator.juniperSurface(world, new BlockPos(0, 100, 0)));
        assertNull(PlantWorldGenerator.juniperSurface(world, new BlockPos(1, 100, 0)));
        assertEquals(new BlockPos(2, 61, 0), PlantWorldGenerator.juniperSurface(world, new BlockPos(2, 100, 0)));
        assertEquals(new BlockPos(3, 61, 0), PlantWorldGenerator.juniperSurface(world, new BlockPos(3, 100, 0)));
        assertEquals(new BlockPos(0, 61, 0), PlantWorldGenerator.motionBlockingSurface(world, new BlockPos(0, 100, 0)));
        assertEquals(new BlockPos(1, 62, 0), PlantWorldGenerator.motionBlockingSurface(world, new BlockPos(1, 100, 0)));
        assertEquals(new BlockPos(4, 0, 0), PlantWorldGenerator.motionBlockingSurface(world, new BlockPos(4, 100, 0)));
    }

    private static final class ColumnWorld extends World {
        private final Chunk chunk;

        private ColumnWorld(ChunkPrimer primer) {
            super(null, new WorldInfo(new WorldSettings(0, GameType.SURVIVAL, false, false, WorldType.DEFAULT), "plant_surface"),
                    new WorldProviderSurface(), new Profiler(), false);
            chunk = new Chunk(this, primer, 0, 0);
        }

        @Override
        public Chunk getChunkFromBlockCoords(BlockPos pos) {
            return chunk;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return x == 0 && z == 0;
        }
    }
}
