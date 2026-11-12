package ivorius.psychedelicraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LatticeBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void fourConnectionBitsRoundTripForBothLatticeTypes() {
        LatticeBlock lattice = new LatticeBlock();
        LatticeBlock crop = new LatticeCropBlock(true, 2, () -> Items.WHEAT);
        for (int mask = 0; mask < 16; mask++) {
            assertEquals(mask, lattice.getMetaFromState(lattice.getStateFromMeta(mask)));
            IBlockState raw = crop.getStateFromMeta(mask);
            assertEquals(mask, crop.getMetaFromState(raw));
            assertEquals(Integer.valueOf(0), raw.getValue(LatticeCropBlock.AGE));
            assertFalse(raw.getValue(LatticeCropBlock.PERSISTENT));
        }
    }

    @Test
    public void transformationsKeepTheLatticeArmsAligned() {
        LatticeBlock lattice = new LatticeBlock();
        IBlockState north = lattice.getStateFromMeta(1 << EnumFacing.NORTH.getHorizontalIndex());
        IBlockState east = lattice.withRotation(north, Rotation.CLOCKWISE_90);
        assertTrue(LatticeBlock.connected(east, EnumFacing.EAST));
        assertFalse(LatticeBlock.connected(east, EnumFacing.NORTH));
        assertTrue(LatticeBlock.connected(lattice.withMirror(east, Mirror.FRONT_BACK), EnumFacing.WEST));
    }

    @Test
    public void rayTraceHitsArmsButMissesTheEmptyCornerOfAJunction() {
        LatticeBlock lattice = new LatticeBlock();
        IBlockState junction = lattice.getStateFromMeta(15);
        assertNotNull(lattice.collisionRayTrace(junction, null, BlockPos.ORIGIN,
                new Vec3d(0.85, 2, 0.5), new Vec3d(0.85, 0, 0.5)));
        assertNull(lattice.collisionRayTrace(junction, null, BlockPos.ORIGIN,
                new Vec3d(0.85, 2, 0.85), new Vec3d(0.85, 0, 0.85)));
    }

    @Test
    public void stemAgeFitsMetadataIndependentlyOfDerivedConnections() {
        VineStemBlock stem = new VineStemBlock();
        for (int age = 0; age <= VineStemBlock.MAX_AGE; age++) {
            IBlockState state = stem.getStateFromMeta(age);
            assertEquals(age, stem.getMetaFromState(LatticeBlock.withConnections(state, 15)));
            assertEquals(0, LatticeBlock.connections(state));
        }
    }
}
