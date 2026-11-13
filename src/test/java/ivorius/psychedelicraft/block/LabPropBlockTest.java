package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

    @Test
    public void trayOverhangHitKeepsItsOwnerAndYieldsToNearerBlocks() {
        LabPropBlock.Tray tray = new LabPropBlock.Tray();
        BlockPos owner = new BlockPos(16, 4, -3);
        Vec3d start = new Vec3d(owner.getX() - 1D / 32, owner.getY() + 2, owner.getZ() + 0.5);
        Vec3d end = start.addVector(0, -3, 0);
        RayTraceResult lip = tray.collisionRayTrace(tray.getDefaultState(), null, owner, start, end);
        assertEquals(owner, lip.getBlockPos());
        assertEquals(EnumFacing.UP, lip.sideHit);
        assertEquals(owner.getY() + 2D / 16, lip.hitVec.y, 0);

        RayTraceResult fartherBlock = new RayTraceResult(end, EnumFacing.UP, owner.west().down());
        RayTraceResult nearerBlock = new RayTraceResult(start.addVector(0, -1, 0), EnumFacing.UP, owner.west().up());
        RayTraceResult miss = new RayTraceResult(RayTraceResult.Type.MISS, start, EnumFacing.UP, owner.west());
        assertSame(lip, TrayRayTrace.nearest(fartherBlock, end, lip));
        assertSame(lip, TrayRayTrace.nearest(miss, end, lip));
        assertSame(nearerBlock, TrayRayTrace.nearest(nearerBlock, end, lip));
        assertSame(miss, TrayRayTrace.nearest(miss, end, null));
        assertNull(TrayRayTrace.nearest(null, end, null));
    }
}
