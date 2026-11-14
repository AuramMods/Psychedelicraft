package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgePlantBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void everyAgeRoundTripsAndOnlyImmatureStagesGrow() {
        assertAges(new NightshadeBlock(() -> Items.AIR, () -> Items.AIR), 7);
        assertAges(new AgavePlantBlock(), 5);
        assertAges(new PeyoteBlock(), 3);
    }

    @Test
    public void peyoteYieldsOneMoreItemPerStageRegardlessOfFortune() {
        PeyoteBlock plant = new PeyoteBlock();
        for (int age = 0; age <= 3; age++) {
            IBlockState state = plant.getStateFromMeta(age);
            assertEquals(age + 1, plant.quantityDropped(state, 0, null));
            assertEquals(age + 1, plant.quantityDropped(state, 3, null));
        }
    }

    @Test
    public void peyoteRequiresSandForPlacementAndSurvival() {
        PeyoteBlock plant = new PeyoteBlock();
        BlockPos pos = new BlockPos(0, 64, 0);
        for (BlockSand.EnumType type : BlockSand.EnumType.values()) {
            World world = plantingWorld(pos, Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, type));
            assertTrue(plant.canPlaceBlockAt(world, pos));
            assertTrue(plant.canBlockStay(world, pos, plant.getDefaultState()));
        }
        for (IBlockState floor : new IBlockState[]{Blocks.STONE.getDefaultState(), Blocks.DIRT.getDefaultState(),
                Blocks.HARDENED_CLAY.getDefaultState(), Blocks.SANDSTONE.getDefaultState()}) {
            World world = plantingWorld(pos, floor);
            assertFalse(plant.canPlaceBlockAt(world, pos));
            assertFalse(plant.canBlockStay(world, pos, plant.getDefaultState()));
        }
    }

    private static World plantingWorld(BlockPos pos, IBlockState support) {
        return new World(null, null, new WorldProviderSurface(), new Profiler(), false) {
            @Override
            public IBlockState getBlockState(BlockPos query) {
                return query.equals(pos.down()) ? support : Blocks.AIR.getDefaultState();
            }

            @Override
            protected IChunkProvider createChunkProvider() { return null; }

            @Override
            protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        };
    }

    private static void assertAges(AgePlantBlock plant, int maxAge) {
        assertEquals(maxAge, plant.maxAge());
        for (IBlockState state : plant.getBlockState().getValidStates()) {
            int metadata = plant.getMetaFromState(state);
            assertEquals(state, plant.getStateFromMeta(metadata));
            if (metadata < maxAge) {
                assertTrue(plant.canGrow(null, null, state, false));
            } else {
                assertFalse(plant.canGrow(null, null, state, false));
            }
        }
        assertEquals(maxAge, plant.getMetaFromState(plant.getStateFromMeta(15)));
    }
}
