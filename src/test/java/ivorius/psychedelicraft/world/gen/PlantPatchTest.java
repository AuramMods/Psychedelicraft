package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.JuniperLeavesBlock;
import ivorius.psychedelicraft.block.TallCropBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    @Test
    public void generatedCanopyUsesNearbyLogsAndMixedFoliage() {
        PatchWorld world = new PatchWorld();
        BlockPos base = new BlockPos(20, 64, 0);
        BlockPos edge = base.add(2, 2, 0);
        IBlockState oldLeaf = PSBlocks.FRUITING_JUNIPER_LEAVES.getDefaultState()
                .withProperty(JuniperLeavesBlock.PERSISTENT, true);
        world.setBlockState(base.down(), Blocks.GRASS.getDefaultState(), 2);
        world.setBlockState(base.add(-1, 2, -2), Blocks.LOG.getDefaultState(), 2);
        world.setBlockState(edge.south(), Blocks.LEAVES.getDefaultState(), 2);
        world.setBlockState(edge.south(2), oldLeaf.withProperty(JuniperLeavesBlock.DISTANCE, 6), 2);
        world.setBlockState(edge.south(3), PSBlocks.JUNIPER_LOG.getDefaultState(), 2);
        world.setBlockState(edge.south(4), oldLeaf, 2);

        assertTrue(JuniperTree.generate(world, new Random(98), base));

        assertSame(Blocks.LOG, world.getBlockState(base.add(-1, 2, -2)).getBlock());
        assertEquals(2, (int) world.getBlockState(base.add(-1, 2, -3)).getValue(JuniperLeavesBlock.DISTANCE));
        assertEquals(4, (int) world.getBlockState(edge).getValue(JuniperLeavesBlock.DISTANCE));
        assertSame(Blocks.LEAVES, world.getBlockState(edge.south()).getBlock());
        assertSame(oldLeaf.withProperty(JuniperLeavesBlock.DISTANCE, 3), world.getBlockState(edge.south(2)));
        assertSame(oldLeaf, world.getBlockState(edge.south(4)));
    }

    @Test
    public void juniperFruitingUsesTheStoredDistanceUntilTheScheduledUpdate() {
        PatchWorld world = new PatchWorld();
        BlockPos pos = new BlockPos(8, 64, 8);
        IBlockState leaf = PSBlocks.JUNIPER_LEAVES.getDefaultState().withProperty(JuniperLeavesBlock.DISTANCE, 4);
        world.setBlockState(pos, leaf, 2);
        Random threshold = new Random() {
            @Override
            public float nextFloat() {
                return 0.01F;
            }
        };
        Random belowThreshold = new Random() {
            @Override
            public float nextFloat() {
                return Math.nextDown(0.01F);
            }
        };

        PSBlocks.JUNIPER_LEAVES.randomTick(world, pos, leaf, threshold);
        assertSame(leaf, world.getBlockState(pos));
        PSBlocks.JUNIPER_LEAVES.randomTick(world, pos, leaf, belowThreshold);
        assertSame(PSBlocks.FRUITING_JUNIPER_LEAVES.getDefaultState().withProperty(JuniperLeavesBlock.DISTANCE, 4),
                world.getBlockState(pos));
    }

    @Test
    public void juniperSaplingNeedsLoadedNeighborsAndLightToAdvance() {
        PatchWorld world = new PatchWorld();
        BlockPos pos = new BlockPos(15, 64, 8);
        IBlockState sapling = PSBlocks.JUNIPER_SAPLING.getDefaultState();
        world.setBlockState(pos.down(), Blocks.MYCELIUM.getDefaultState(), 2);
        world.setBlockState(pos, sapling, 2);
        Random successfulTick = new Random() {
            @Override
            public int nextInt(int bound) {
                assertEquals(7, bound);
                return 0;
            }
        };
        assertTrue(PSBlocks.JUNIPER_SAPLING.canBlockStay(world, pos, sapling));

        world.eastChunkLoaded = false;
        world.light = 15;
        PSBlocks.JUNIPER_SAPLING.updateTick(world, pos, sapling, successfulTick);
        assertSame(sapling, world.getBlockState(pos));
        assertEquals(0, world.lightReads);

        world.eastChunkLoaded = true;
        world.light = 8;
        PSBlocks.JUNIPER_SAPLING.updateTick(world, pos, sapling, successfulTick);
        assertSame(sapling, world.getBlockState(pos));
        world.light = 9;
        PSBlocks.JUNIPER_SAPLING.updateTick(world, pos, sapling, successfulTick);
        assertSame(sapling.withProperty(BlockSapling.STAGE, 1), world.getBlockState(pos));
    }

    @Test
    public void beeNestPlacementUsesLowestLeavesAndNeedsAClearSouthExit() {
        PatchWorld world = new PatchWorld();
        BlockPos trunk = new BlockPos(7, 64, 7);
        BlockPos height = trunk.up(2);
        IBlockState stone = Blocks.STONE.getDefaultState();
        world.setBlockState(height.east().south(), stone, 2);
        world.setBlockState(height.south(2), stone, 2);

        BeeNestWorldGenerator.placeNest(world, new HashSet<>(Arrays.asList(trunk, trunk.up(), height)),
                Collections.singleton(trunk.west().up(3)), new Random(179));

        assertSame(PSBlocks.BEE_NEST, world.getBlockState(height.west()).getBlock());
        assertSame(Blocks.AIR, world.getBlockState(height.east()).getBlock());
        assertSame(Blocks.AIR, world.getBlockState(height.north()).getBlock());
        assertSame(Blocks.AIR, world.getBlockState(trunk.west().up()).getBlock());
    }

    @Test
    public void beeNestRatesFollowTheLegacyBiomeCounterparts() {
        assertEquals(0.05F, BeeNestWorldGenerator.naturalChance(Biomes.PLAINS), 0F);
        assertEquals(0.05F, BeeNestWorldGenerator.naturalChance(Biomes.MUTATED_PLAINS), 0F);
        assertEquals(0.02F, BeeNestWorldGenerator.naturalChance(Biomes.MUTATED_FOREST), 0F);
        assertEquals(0.002F, BeeNestWorldGenerator.naturalChance(Biomes.FOREST), 0F);
        assertEquals(0.002F, BeeNestWorldGenerator.naturalChance(Biomes.MUTATED_BIRCH_FOREST), 0F);
        assertEquals(0F, BeeNestWorldGenerator.naturalChance(Biomes.ROOFED_FOREST), 0F);
        assertEquals(0F, BeeNestWorldGenerator.naturalChance(Biomes.SWAMPLAND), 0F);
    }

    private static final class PatchWorld extends World {
        private final Map<BlockPos, IBlockState> blocks = new HashMap<>();
        private boolean eastChunkLoaded = true;
        private int light;
        private int lightReads;

        private PatchWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.getDefaultState());
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return null;
        }

        @Override
        public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
            blocks.put(pos, state);
            return true;
        }

        @Override
        public int getLight(BlockPos pos, boolean checkNeighbors) {
            assertFalse(checkNeighbors);
            lightReads++;
            return light;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return x != 1 || eastChunkLoaded;
        }
    }
}
