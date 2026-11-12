package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    @Test
    public void ceilingSignSavesEveryRotationAndSelectsCardinalBoards() {
        JuniperSignBlock block = new JuniperHangingSigns.Ceiling();
        for (int metadata = 0; metadata < 16; metadata++) {
            IBlockState state = block.getStateFromMeta(metadata);
            assertEquals(metadata, block.getMetaFromState(state));
            AxisAlignedBB outline = state.getBoundingBox(null, BlockPos.ORIGIN);
            assertEquals((metadata & 3) == 0 ? 10 / 16.0 : 1.0, outline.maxY, 0);
            assertNull(state.getCollisionBoundingBox(null, BlockPos.ORIGIN));
        }
    }

    @Test
    public void wallHangingSignRayTraceSkipsTheGapAndCollidesOnlyWithItsBeam() {
        JuniperSignBlock block = new JuniperHangingSigns.Wall();
        BlockPos origin = BlockPos.ORIGIN;
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            IBlockState state = block.getDefaultState().withProperty(JuniperSignBlock.FACING, direction);
            assertSame(state, block.getStateFromMeta(block.getMetaFromState(state)));
            AxisAlignedBB beam = state.getCollisionBoundingBox(null, origin);
            assertEquals(14 / 16.0, beam.minY, 0);
            boolean acrossX = direction.getAxis() == EnumFacing.Axis.Z;
            assertEquals(acrossX ? 1.0 : 0.25, beam.maxX - beam.minX, 0);
            assertEquals(acrossX ? 0.25 : 1.0, beam.maxZ - beam.minZ, 0);
        }
        IBlockState north = block.getDefaultState();
        assertNotNull(block.collisionRayTrace(north, null, origin, new Vec3d(0.5, 0.4, -1), new Vec3d(0.5, 0.4, 2)));
        assertNull(block.collisionRayTrace(north, null, origin, new Vec3d(0.5, 0.75, -1), new Vec3d(0.5, 0.75, 2)));
        assertNotNull(block.collisionRayTrace(north, null, origin, new Vec3d(0.5, 0.95, -1), new Vec3d(0.5, 0.95, 2)));
    }
}
