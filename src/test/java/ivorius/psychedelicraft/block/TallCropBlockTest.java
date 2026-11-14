package ivorius.psychedelicraft.block;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
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

public class TallCropBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void eachCropStateHasUniqueFourBitMetadata() {
        assertMetadata(new TallCropBlock(TallCropBlock.Crop.CANNABIS, false), 16);
        assertMetadata(new TallCropBlock(TallCropBlock.Crop.CANNABIS, true), 16);
        assertMetadata(new TallCropBlock(TallCropBlock.Crop.HOP, false), 16);
        assertMetadata(new TallCropBlock.Coca(false), 13);
        assertMetadata(new TallCropBlock.TwoPart(TallCropBlock.Crop.TOBACCO, false), 16);
        assertMetadata(new TallCropBlock.TwoPart(TallCropBlock.Crop.COFFEA, false), 16);
    }

    @Test
    public void coffeaUpperSectionFinishesGrowingEarlier() {
        TallCropBlock coffea = new TallCropBlock.TwoPart(TallCropBlock.Crop.COFFEA, false);
        assertEquals(7, coffea.maxAge(coffea.stateForHeight(0)));
        assertEquals(3, coffea.maxAge(coffea.stateForHeight(1)));
    }

    @Test
    public void wildTallCropsKeepTheirSpeciesGrowthAndSoilRules() {
        TallCropBlock wildCoffea = new TallCropBlock.TwoPart(TallCropBlock.Crop.COFFEA, true);
        assertEquals(7, wildCoffea.getMetaFromState(wildCoffea.stateForHeight(0, 15)));
        assertEquals(11, wildCoffea.getMetaFromState(wildCoffea.stateForHeight(1, 15)));
        assertTrue(wildCoffea.canSustainBush(Blocks.GRASS.getDefaultState()));
        assertTrue(wildCoffea.canSustainBush(Blocks.MYCELIUM.getDefaultState()));
        assertTrue(!wildCoffea.canSustainBush(Blocks.FARMLAND.getDefaultState()));
        assertMetadata(new TallCropBlock.Coca(true), 13);
        assertMetadata(wildCoffea, 16);
    }

    @Test
    public void cocaSeedsRequireSoilEvenThoughStalksSupportFurtherGrowth() {
        TallCropBlock crop = new TallCropBlock.Coca(false);
        BlockPos pos = new BlockPos(0, 64, 0);
        for (IBlockState soil : new IBlockState[]{Blocks.FARMLAND.getDefaultState(), Blocks.DIRT.getDefaultState(),
                Blocks.GRASS.getDefaultState(), Blocks.MYCELIUM.getDefaultState()}) {
            assertTrue(crop.canPlaceBlockAt(plantingWorld(pos, soil), pos));
        }
        assertFalse(crop.canPlaceBlockAt(plantingWorld(pos, Blocks.STONE.getDefaultState()), pos));
        for (TallCropBlock stalk : new TallCropBlock[]{crop, new TallCropBlock.Coca(true)}) {
            World world = plantingWorld(pos, stalk.getDefaultState());
            assertFalse(crop.canPlaceBlockAt(world, pos));
            assertTrue(crop.canBlockStay(world, pos, crop.stateForHeight(1)));
        }
    }

    private static World plantingWorld(BlockPos pos, IBlockState support) {
        return new World(null, null, new WorldProviderSurface(), new Profiler(), false) {
            @Override
            public IBlockState getBlockState(BlockPos query) {
                return query.equals(pos.down()) ? support : Blocks.AIR.getDefaultState();
            }

            @Override
            public int getLight(BlockPos query) { return 15; }

            @Override
            protected IChunkProvider createChunkProvider() { return null; }

            @Override
            protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        };
    }

    private static void assertMetadata(TallCropBlock crop, int states) {
        Set<Integer> metadata = new HashSet<>();
        for (IBlockState state : crop.getBlockState().getValidStates()) {
            int encoded = crop.getMetaFromState(state);
            assertTrue(encoded >= 0 && encoded <= 15);
            assertTrue(metadata.add(encoded));
            assertEquals(state, crop.getStateFromMeta(encoded));
        }
        assertEquals(states, metadata.size());
    }
}
