package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.TallCropBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PlantPatchTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void patchSamplesAgesAfterTheAirFilterAndBeforeTheSupportCheck() {
        PatchWorld world = new PatchWorld();
        BlockPos origin = new BlockPos(8, 64, 8);
        IBlockState grass = Blocks.TALLGRASS.getStateFromMeta(1);
        IBlockState addonAir = new Block(Material.AIR) {
            @Override
            public boolean isAir(IBlockState state, IBlockAccess world, BlockPos pos) {
                return true;
            }
        }.getDefaultState();
        world.setBlockState(origin, grass, 2);
        world.setBlockState(origin.east(3), addonAir, 2);
        for (int column : new int[]{0, 2, 3, 4}) {
            world.setBlockState(origin.east(column).down(), Blocks.STONE.getDefaultState(), 2);
        }
        int[] ages = {5, 4, 1};
        Random random = new Random() {
            private int offsets;
            private int samples;

            @Override
            public int nextInt(int bound) {
                if (bound == 6) {
                    return ages[samples++];
                }
                int offset = offsets++;
                return offset % 6 == 0 ? offset / 6 : 0;
            }
        };

        PlantWorldGenerator.placeLowPatch(world, origin, random, PSBlocks.AGAVE_PLANT, PSBlocks.AGAVE_PLANT.maxAge());

        assertSame(grass, world.getBlockState(origin));
        assertSame(Blocks.AIR, world.getBlockState(origin.east()).getBlock());
        assertEquals(PSBlocks.AGAVE_PLANT.getStateFromMeta(4), world.getBlockState(origin.east(2)));
        assertSame(addonAir, world.getBlockState(origin.east(3)));
        assertEquals(PSBlocks.AGAVE_PLANT.getStateFromMeta(1), world.getBlockState(origin.east(4)));
    }

    @Test
    public void tallPatchFindsSoilBeneathJuniperLeavesAndUsesSupportForHigherSections() {
        PatchWorld world = new PatchWorld();
        BlockPos base = new BlockPos(8, 64, 8);
        world.setBlockState(base.down(), Blocks.GRASS.getDefaultState(), 2);
        world.setBlockState(base, PSBlocks.JUNIPER_LEAVES.getDefaultState(), 2);
        world.setBlockState(base.up(), Blocks.LEAVES.getDefaultState(), 2);
        world.setBlockState(base.up(2), Blocks.COBBLESTONE.getDefaultState(), 2);
        TallCropBlock crop = PSBlocks.NATURAL_CANNABIS;
        Random random = new Random() {
            private final int[] draws = {0, 0, 1, 1, 6};
            private int index;

            @Override
            public int nextInt(int bound) {
                int value = draws[index++];
                if (value >= bound) throw new AssertionError("draw " + value + " for bound " + bound);
                return value;
            }
        };

        PlantWorldGenerator.placeTallPatch(world, base.up(), random, crop, false);

        assertSame(Blocks.GRASS, world.getBlockState(base.down()).getBlock());
        assertSame(crop.stateForHeight(0, 15), world.getBlockState(base));
        assertSame(crop.stateForHeight(1, 15), world.getBlockState(base.up()));
        assertSame(crop.stateForHeight(2, 6), world.getBlockState(base.up(2)));
    }

    @Test
    public void juniperForkUsesItsOriginalLevelBudgetAndSkipsAnObstructedLog() {
        PatchWorld world = new PatchWorld();
        BlockPos base = new BlockPos(8, 64, 8);
        BlockPos obstruction = base.north(3).up(3);
        world.setBlockState(base.down(), Blocks.GRASS.getDefaultState(), 2);
        world.setBlockState(obstruction, Blocks.COBBLESTONE.getDefaultState(), 2);

        assertTrue(JuniperTree.generate(world, new Random(98), base));

        assertSame(Blocks.COBBLESTONE, world.getBlockState(obstruction).getBlock());
        assertSame(PSBlocks.JUNIPER_LOG, world.getBlockState(base.north(3).up(4)).getBlock());
        assertSame(PSBlocks.JUNIPER_LOG, world.getBlockState(base.south().up()).getBlock());
        assertFalse(world.getBlockState(base.south(2).up(2)).getBlock() == PSBlocks.JUNIPER_LOG);
    }

    @Test
    public void vineInTheClearanceVolumeStopsJuniperGrowth() {
        PatchWorld world = new PatchWorld();
        BlockPos base = new BlockPos(8, 64, 8);
        IBlockState grass = Blocks.GRASS.getDefaultState();
        world.setBlockState(base.down(), grass, 2);
        world.setBlockState(base.up().east(), Blocks.VINE.getStateFromMeta(1), 2);

        assertFalse(JuniperTree.generate(world, new Random(98), base));

        assertSame(grass, world.getBlockState(base.down()));
        assertSame(Blocks.AIR, world.getBlockState(base).getBlock());
    }

    private static final class PatchWorld extends World {
        private final Map<BlockPos, IBlockState> blocks = new HashMap<>();

        private PatchWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.getDefaultState());
        }

        @Override
        public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
            blocks.put(pos, state);
            return true;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }
    }
}
