package ivorius.psychedelicraft.block;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.Rotation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JuniperBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void leafDistanceAndPersistenceFitInChunkMetadata() {
        for (boolean fruiting : new boolean[]{false, true}) {
            JuniperLeavesBlock leaves = new JuniperLeavesBlock(fruiting);
            Set<Integer> metadata = new HashSet<>();
            for (IBlockState state : leaves.getBlockState().getValidStates()) {
                int value = leaves.getMetaFromState(state);
                assertTrue(value >= 0 && value < 16);
                assertTrue(metadata.add(value));
                assertSame(state, leaves.getStateFromMeta(value));
            }
            assertEquals(14, metadata.size());
        }
    }

    @Test
    public void logAxisRoundTripsAndUsesNativePillarRotation() {
        JuniperLogBlock log = new JuniperLogBlock(null);
        for (BlockLog.EnumAxis axis : BlockLog.EnumAxis.values()) {
            IBlockState state = log.getDefaultState().withProperty(BlockLog.LOG_AXIS, axis);
            assertSame(state, log.getStateFromMeta(log.getMetaFromState(state)));
        }
        IBlockState rotated = log.getDefaultState().withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.X)
                .withRotation(Rotation.CLOCKWISE_90);
        assertEquals(BlockLog.EnumAxis.Z, rotated.getValue(BlockLog.LOG_AXIS));
    }

    @Test
    public void saplingKeepsItsGrowthStage() {
        JuniperSaplingBlock sapling = new JuniperSaplingBlock();
        for (int stage = 0; stage < 2; stage++) {
            IBlockState state = sapling.getDefaultState().withProperty(BlockSapling.STAGE, stage);
            assertSame(state, sapling.getStateFromMeta(sapling.getMetaFromState(state)));
        }
    }
}
