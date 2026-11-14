package ivorius.psychedelicraft.fluid.physical;

import net.minecraft.block.BlockStoneSlab;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OceanWaterloggingTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void oceanTemplateShapesUseWaterTilesAndKeepChestInventories() {
        assertTrue(Waterlogging.needsWaterTile(Blocks.WOODEN_SLAB.getDefaultState()));
        assertTrue(Waterlogging.needsWaterTile(Blocks.SANDSTONE_STAIRS.getDefaultState()));
        assertTrue(Waterlogging.needsWaterTile(Blocks.OAK_FENCE.getDefaultState()));
        assertTrue(Waterlogging.needsWaterTile(Blocks.TRAPDOOR.getDefaultState()));
        assertTrue(Waterlogging.supports(Blocks.CHEST.getDefaultState()));
        assertFalse(Waterlogging.needsWaterTile(Blocks.CHEST.getDefaultState()));
        assertFalse(Waterlogging.needsWaterTile(Blocks.TRAPPED_CHEST.getDefaultState()));
    }

    @Test
    public void doubleSlabsAndUnrelatedStoneSlabsKeepTheirLegacyBehavior() {
        assertTrue(Waterlogging.needsWaterTile(Blocks.STONE_SLAB.getDefaultState()
                .withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.SAND)));
        assertFalse(Waterlogging.supports(Blocks.STONE_SLAB.getDefaultState()
                .withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.STONE)));
        assertFalse(Waterlogging.supports(Blocks.DOUBLE_STONE_SLAB.getDefaultState()));
        assertFalse(Waterlogging.supports(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState()));
    }
}
