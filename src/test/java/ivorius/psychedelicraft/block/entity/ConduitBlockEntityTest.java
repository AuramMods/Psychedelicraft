package ivorius.psychedelicraft.block.entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConduitBlockEntityTest {
    @Test
    public void threeIntersectingRingsHaveExactlyFortyTwoPositions() {
        int frameBlocks = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (ConduitBlockEntity.isFrameOffset(x, y, z)) frameBlocks++;
                }
            }
        }
        assertEquals(42, frameBlocks);
        assertTrue(ConduitBlockEntity.isFrameOffset(0, 2, -2));
        assertFalse(ConduitBlockEntity.isFrameOffset(2, 2, 2));
        assertFalse(ConduitBlockEntity.isFrameOffset(0, 1, 1));
        assertFalse(ConduitBlockEntity.isFrameOffset(0, 0, 3));
    }

    @Test
    public void rangeIncreasesInSixteenBlockSteps() {
        assertEquals(32, ConduitBlockEntity.effectRange(16));
        assertEquals(32, ConduitBlockEntity.effectRange(20));
        assertEquals(48, ConduitBlockEntity.effectRange(21));
        assertEquals(80, ConduitBlockEntity.effectRange(41));
        assertEquals(96, ConduitBlockEntity.effectRange(42));
    }
}
