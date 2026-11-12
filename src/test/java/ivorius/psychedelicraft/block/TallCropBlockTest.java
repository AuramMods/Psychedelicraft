package ivorius.psychedelicraft.block;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
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
        assertMetadata(new TallCropBlock.Coca(), 13);
        assertMetadata(new TallCropBlock.TwoPart(TallCropBlock.Crop.TOBACCO), 16);
        assertMetadata(new TallCropBlock.TwoPart(TallCropBlock.Crop.COFFEA), 16);
    }

    @Test
    public void coffeaUpperSectionFinishesGrowingEarlier() {
        TallCropBlock coffea = new TallCropBlock.TwoPart(TallCropBlock.Crop.COFFEA);
        assertEquals(7, coffea.maxAge(coffea.stateForHeight(0)));
        assertEquals(3, coffea.maxAge(coffea.stateForHeight(1)));
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
