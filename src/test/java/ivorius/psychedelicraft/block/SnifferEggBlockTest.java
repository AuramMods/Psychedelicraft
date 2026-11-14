package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class SnifferEggBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void allThreeCrackModelsRoundTripThroughLegacyMetadata() {
        SnifferEggBlock egg = new SnifferEggBlock();
        for (IBlockState state : egg.getBlockState().getValidStates()) {
            assertEquals(state, egg.getStateFromMeta(egg.getMetaFromState(state)));
        }
        assertEquals(0, egg.getMetaFromState(egg.getDefaultState()));
        assertEquals(2, egg.getMetaFromState(egg.getStateFromMeta(15)));
    }

    @Test
    public void mossReducesEachStageByFourThousandTicksWithTheSameRandomOffset() {
        Random last = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };
        assertEquals(8299, SnifferEggBlock.stageInterval(false, last));
        assertEquals(4299, SnifferEggBlock.stageInterval(true, last));
    }
}
