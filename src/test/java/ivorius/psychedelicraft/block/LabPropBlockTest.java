package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LabPropBlockTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void trayMetadataPreservesTheAxisAndOverhang() {
        LabPropBlock.Tray tray = new LabPropBlock.Tray();
        IBlockState alongX = tray.getDefaultState();
        IBlockState alongZ = alongX.withProperty(LabPropBlock.Tray.AXIS, EnumFacing.Axis.Z);
        assertSame(alongX, tray.getStateFromMeta(tray.getMetaFromState(alongX)));
        assertSame(alongZ, tray.getStateFromMeta(tray.getMetaFromState(alongZ)));
        assertEquals(new AxisAlignedBB(-1 / 16D, 0, 2 / 16D, 17 / 16D, 2 / 16D, 14 / 16D),
                tray.getBoundingBox(alongX, null, BlockPos.ORIGIN));
        assertEquals(new AxisAlignedBB(2 / 16D, 0, -1 / 16D, 14 / 16D, 2 / 16D, 17 / 16D),
                tray.getBoundingBox(alongZ, null, BlockPos.ORIGIN));
    }

    @Test
    public void burnerKeepsTheCenteredFootprint() {
        Block burner = LabPropBlock.burner();
        assertEquals(new AxisAlignedBB(3 / 16D, 0, 3 / 16D, 13 / 16D, 2 / 16D, 13 / 16D),
                burner.getBoundingBox(burner.getDefaultState(), null, BlockPos.ORIGIN));
    }
}
