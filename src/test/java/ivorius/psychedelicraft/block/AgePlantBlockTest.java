package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
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
