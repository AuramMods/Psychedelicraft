package ivorius.psychedelicraft.block;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
