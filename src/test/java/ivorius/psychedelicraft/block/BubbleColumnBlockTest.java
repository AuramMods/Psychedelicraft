package ivorius.psychedelicraft.block;

import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BubbleColumnBlockTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void directionSurvivesMetadataAsSourceWater() {
        BubbleColumnBlock column = new BubbleColumnBlock();
        assertFalse(column.getStateFromMeta(0).getValue(BubbleColumnBlock.DRAG_DOWN));
        assertTrue(column.getStateFromMeta(1).getValue(BubbleColumnBlock.DRAG_DOWN));
        for (int meta = 0; meta <= 1; meta++) {
            assertEquals(meta, column.getMetaFromState(column.getStateFromMeta(meta)));
            assertEquals(0, column.getStateFromMeta(meta).getValue(BlockLiquid.LEVEL).intValue());
        }
    }

    @Test
    public void submergedCurrentClearsFallDistanceAndUsesDifferentLimitsFromTheSurface() {
        EntityBoat boat = new EntityBoat(null);
        boat.fallDistance = 6;
        boat.motionY = 0.68;
        BubbleColumnBlock.moveEntity(boat, false, false);
        assertEquals(0.7, boat.motionY, 0.000001);
        assertEquals(0, boat.fallDistance, 0);

        boat.fallDistance = 6;
        boat.motionY = -0.89;
        BubbleColumnBlock.moveEntity(boat, true, true);
        assertEquals(-0.9, boat.motionY, 0.000001);
        assertEquals(6, boat.fallDistance, 0);
    }
}
