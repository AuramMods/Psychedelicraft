package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class JuniperSignBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void standingSignKeepsAllSixteenAnglesAndRotatesByAQuarterTurn() {
        JuniperSignBlock block = new JuniperSignBlock.Standing();
        for (int metadata = 0; metadata < 16; metadata++) {
            IBlockState state = block.getStateFromMeta(metadata);
            assertEquals(metadata, block.getMetaFromState(state));
            assertEquals((metadata + 4) & 15, block.getMetaFromState(state.withRotation(Rotation.CLOCKWISE_90)));
        }
    }

    @Test
    public void wallSignKeepsItsHorizontalFace() {
        JuniperSignBlock block = new JuniperSignBlock.Wall();
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            IBlockState state = block.getDefaultState().withProperty(JuniperSignBlock.FACING, direction);
            assertSame(state, block.getStateFromMeta(block.getMetaFromState(state)));
        }
    }
}
