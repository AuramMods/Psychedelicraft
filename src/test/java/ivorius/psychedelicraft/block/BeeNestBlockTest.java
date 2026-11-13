package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BeeNestBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void honeyLevelsRoundTripThroughMetadataAndComparator() {
        BeeNestBlock nest = new BeeNestBlock();
        for (int level = 0; level <= BeeNestBlock.FULL_HONEY; level++) {
            IBlockState state = nest.getDefaultState().withProperty(BeeNestBlock.HONEY_LEVEL, level);
            assertEquals(level, nest.getMetaFromState(state));
            assertEquals(state, nest.getStateFromMeta(level));
            assertEquals(level, nest.getComparatorInputOverride(state, null, null));
        }
        assertEquals(nest.getDefaultState(), nest.getStateFromMeta(15));
    }
}
