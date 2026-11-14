package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlitchedBlockTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void airFlagPreservesSolidCollisionAndNonopaqueLighting() {
        GlitchedBlock block = new GlitchedBlock();
        IBlockState state = block.getDefaultState();
        BlockPos pos = BlockPos.ORIGIN;

        assertTrue(block.isAir(state, null, pos));
        assertFalse(state.getMaterial().isReplaceable());
        assertTrue(state.isNormalCube());
        assertTrue(state.isTopSolid());
        assertTrue(state.causesSuffocation());
        assertEquals(Block.FULL_BLOCK_AABB, state.getCollisionBoundingBox(null, pos));
        assertFalse(state.isOpaqueCube());
        assertEquals(1, state.getLightOpacity(null, pos));
        assertEquals(0, state.getLightValue(null, pos));
        assertEquals(MapColor.BLACK, state.getMapColor(null, pos));
    }
}
