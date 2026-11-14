package ivorius.psychedelicraft.world.gen;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuriedTreasureTest {
    @Test
    public void buriedTreasureUsesTheSeededLegacyChunkRoll() {
        assertTrue(BuriedTreasure.rollsTreasure(23842L, 104, -137));
        assertTrue(BuriedTreasure.rollsTreasure(23842L, -39, -12));
        assertFalse(BuriedTreasure.rollsTreasure(23842L, 104, -136));
        assertFalse(BuriedTreasure.rollsTreasure(23842L, -39, -11));
    }

    @Test
    public void bothPositiveAndNegativeChunksUseTheDonorAnchor() {
        assertEquals(new BlockPos(1673, 0, -2183), BuriedTreasure.anchor(104, -137));
        assertEquals(new BlockPos(-615, 0, -183), BuriedTreasure.anchor(-39, -12));
    }
}
